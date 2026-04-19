import javax.swing.*;
import ui.AppTheme;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use cross-platform LAF so custom colours render correctly on all OSes
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException |
                     IllegalAccessException | UnsupportedLookAndFeelException ignored) {}
            AppTheme.apply();
            new MainFrame().setVisible(true);
        });
    }
}
