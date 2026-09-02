package br.com.entremitoseraizes.dialogue;

/** Uma fala curta. Separar falas mantém o texto legível em qualquer tamanho. */
public final class DialogueLine {
    public final String speaker;
    public final String text;

    public DialogueLine(String speaker, String text) {
        this.speaker = speaker;
        this.text = text;
    }
}
