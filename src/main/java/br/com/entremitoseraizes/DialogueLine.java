package br.com.entremitoseraizes;

/** Uma fala curta. Separar falas mantém o texto legível em qualquer tamanho. */
final class DialogueLine {
    final String speaker;
    final String text;

    DialogueLine(String speaker, String text) {
        this.speaker = speaker;
        this.text = text;
    }
}
