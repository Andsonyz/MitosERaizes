package br.com.entremitoseraizes;

/** Cada etapa corresponde a uma mecânica apresentada na proposta da fase. */
enum Stage {
    FIND_CABIN("Encontre a cabana na mata e peça orientação ao guardião."),
    INVESTIGATE("Investigue as três pistas da área destruída."),
    FOLLOW_TRACKS("Siga os rastros até a bifurcação."),
    CHOOSE_PATH("Descubra o caminho verdadeiro observando a mata."),
    INVESTIGATE_CAMP("Examine o acampamento dos invasores."),
    RESCUE_ANIMAL("Liberte o animal preso na armadilha."),
    ESCORT_ANIMAL("Leve o animal para a clareira segura."),
    FALSE_TRAIL("Prepare uma trilha falsa com o Curupira."),
    RACE_TO_TREE("Corra até a árvore ancestral antes dos invasores."),
    CONFRONT("Ajude a expulsar os invasores sem violência."),
    RESTORE("Reconstrua a parte ferida da floresta."),
    COMPLETE("Fase concluída.");

    private final String objective;

    Stage(String objective) { this.objective = objective; }
    String objective() { return objective; }
}
