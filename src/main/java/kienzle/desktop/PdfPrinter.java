package kienzle.desktop;

import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Prints a PDF through the JavaFX print dialog, independent of the operating system. */
final class PdfPrinter {
    private static final float RENDER_DPI = 200f;

    /** @return {@code true} if the pages were sent to the printer, {@code false} if the user cancelled. */
    boolean print(Path pdf, Window owner) throws IOException {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            throw new IllegalStateException(I18n.get("pdf.print.noprinter"));
        }
        if (!job.showPrintDialog(owner)) {
            return false;
        }
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            PageLayout layout = job.getJobSettings().getPageLayout();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                ImageView view = pageView(renderer.renderImageWithDPI(page, RENDER_DPI), layout);
                if (!job.printPage(layout, view)) {
                    job.cancelJob();
                    throw new IllegalStateException(I18n.get("pdf.print.failed") + pdf);
                }
            }
        }
        if (!job.endJob()) {
            throw new IllegalStateException(I18n.get("pdf.print.failed") + pdf);
        }
        return true;
    }

    private static ImageView pageView(BufferedImage image, PageLayout layout) {
        ImageView view = new ImageView(toFxImage(image));
        view.setPreserveRatio(true);
        view.setFitWidth(layout.getPrintableWidth());
        view.setFitHeight(layout.getPrintableHeight());
        return view;
    }

    private static WritableImage toFxImage(BufferedImage image) {
        WritableImage fxImage = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter writer = fxImage.getPixelWriter();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                writer.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return fxImage;
    }
}
