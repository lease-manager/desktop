package kienzle.desktop.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** QR code encoding and decoding (ZXing). */
public final class QrCodes {
    private static final int[] SEARCH_SIZES = {120, 160, 200, 260, 320, 400};

    private QrCodes() {
    }

    public static BufferedImage encode(String text, int sizePx) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Optional<String> decode(BufferedImage image) {
        Optional<String> direct = tryDecode(image);
        if (direct.isPresent()) {
            return direct;
        }

        Optional<String> thresholded = tryDecode(threshold(image, 180));
        if (thresholded.isPresent()) {
            return thresholded;
        }

        // The stamper places the code top right; PDFs stamped by older versions have it bottom left.
        int width = image.getWidth();
        int height = image.getHeight();
        Optional<String> topRight = scanRegion(image, width * 3 / 5, 0, width, height / 4);
        if (topRight.isPresent()) {
            return topRight;
        }
        return scanRegion(image, 0, height * 3 / 4, width * 2 / 5, height);
    }

    private static Optional<String> scanRegion(BufferedImage image, int left, int top, int right, int bottom) {
        int step = Math.max(10, image.getWidth() / 80);

        for (int size : SEARCH_SIZES) {
            if (size > Math.min(right - left, bottom - top)) {
                continue;
            }
            for (int x = left; x + size <= right; x += step) {
                for (int y = top; y + size <= bottom; y += step) {
                    BufferedImage crop = image.getSubimage(x, y, size, size);
                    Optional<String> decoded = tryDecode(threshold(crop, 180));
                    if (decoded.isPresent()) {
                        return decoded;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> tryDecode(BufferedImage image) {
        var source = new BufferedImageLuminanceSource(image);
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            var result = new MultiFormatReader().decode(bitmap, Map.of(
                    DecodeHintType.TRY_HARDER, Boolean.TRUE,
                    DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE),
                    DecodeHintType.PURE_BARCODE, Boolean.FALSE));
            return Optional.of(result.getText());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private static BufferedImage threshold(BufferedImage image, int cutoff) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
                copy.setRGB(x, y, luminance > cutoff ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return copy;
    }
}
