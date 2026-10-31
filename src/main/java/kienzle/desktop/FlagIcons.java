package kienzle.desktop;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/** Draws the flags used by the language buttons with vector shapes. */
final class FlagIcons {
    private static final double WIDTH = 30.0;
    private static final double HEIGHT = 20.0;

    private FlagIcons() {
    }

    static Node germany() {
        double stripe = HEIGHT / 3.0;
        return new Group(
                rect(0, 0, WIDTH, stripe, Color.BLACK),
                rect(0, stripe, WIDTH, stripe, Color.web("#DD0000")),
                rect(0, 2 * stripe, WIDTH, stripe, Color.web("#FFCE00")));
    }

    static Node unitedKingdom() {
        Color blue = Color.web("#012169");
        Color red = Color.web("#C8102E");
        Group flag = new Group(
                rect(0, 0, WIDTH, HEIGHT, blue),
                diagonals(3.6, Color.WHITE),
                diagonals(1.6, red),
                rect(WIDTH / 2 - 3.5, 0, 7, HEIGHT, Color.WHITE),
                rect(0, HEIGHT / 2 - 3.5, WIDTH, 7, Color.WHITE),
                rect(WIDTH / 2 - 2, 0, 4, HEIGHT, red),
                rect(0, HEIGHT / 2 - 2, WIDTH, 4, red));
        flag.setClip(new Rectangle(WIDTH, HEIGHT));
        return flag;
    }

    private static Rectangle rect(double x, double y, double w, double h, Color color) {
        Rectangle rectangle = new Rectangle(x, y, w, h);
        rectangle.setFill(color);
        return rectangle;
    }

    private static Node diagonals(double thickness, Color color) {
        double t = thickness / 2.0;
        Polygon down = new Polygon(-t, 0, t, 0, WIDTH + t, HEIGHT, WIDTH - t, HEIGHT);
        Polygon up = new Polygon(WIDTH - t, 0, WIDTH + t, 0, t, HEIGHT, -t, HEIGHT);
        down.setFill(color);
        up.setFill(color);
        return new Group(down, up);
    }
}
