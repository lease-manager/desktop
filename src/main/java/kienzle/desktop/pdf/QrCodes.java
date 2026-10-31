package kienzle.desktop.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;

/** QR code encoding and decoding (ZXing). */
public final class QrCodes {
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
        var source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
        var bitmap = new BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        try {
            return Optional.of(new MultiFormatReader()
                    .decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE,
                            DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(BarcodeFormat.QR_CODE)))
                    .getText());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
