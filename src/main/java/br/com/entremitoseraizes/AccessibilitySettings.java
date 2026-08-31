package br.com.entremitoseraizes;

/** Preferências de leitura: nunca dependem apenas de cor ou áudio. */
final class AccessibilitySettings {
    private int textScale = 1;
    private boolean highContrast;

    int textScale() { return textScale; }
    boolean highContrast() { return highContrast; }

    void increaseText() { if (textScale < 2) textScale++; }
    void decreaseText() { if (textScale > 0) textScale--; }
    void toggleContrast() { highContrast = !highContrast; }

    String textSizeName() {
        return textScale == 0 ? "Compacto" : textScale == 1 ? "Padrão" : "Ampliado";
    }
}
