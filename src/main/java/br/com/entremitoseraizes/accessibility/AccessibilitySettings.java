package br.com.entremitoseraizes.accessibility;

/** Preferências de leitura: nunca dependem apenas de cor ou áudio. */
public final class AccessibilitySettings {
    public int textScale = 1;
    public boolean highContrast;

    public int textScale() { return textScale; }
    public boolean highContrast() { return highContrast; }

    public void increaseText() { if (textScale < 2) textScale++; }
    public void decreaseText() { if (textScale > 0) textScale--; }
    public void toggleContrast() { highContrast = !highContrast; }

    public String textSizeName() {
        return textScale == 0 ? "Compacto" : textScale == 1 ? "Padrão" : "Ampliado";
    }
}
