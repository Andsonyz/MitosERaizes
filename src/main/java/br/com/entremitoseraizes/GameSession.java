package br.com.entremitoseraizes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regras e conteúdo da campanha. A camada visual consulta este objeto e não
 * precisa conhecer os detalhes das quatro missões.
 */
final class GameSession {
    static final int LAST_CHAPTER = 3;

    private static final String[] CHAPTER_NAMES = {
        "I. Fagulhas de alerta", "II. Trilhas que protegem", "III. O canto das águas", "IV. O círculo de fogo"
    };
    private static final String[] GUARDIANS = { "Saci", "Caipora", "Iara", "Boitatá" };
    private static final String[] OBJECTIVES = {
        "Investigue três sinais de risco de queimada.",
        "Ajude a Caipora a restaurar três pontos da trilha.",
        "Proteja as nascentes com a orientação de Iara.",
        "Organize uma resposta segura para conter o fogo."
    };

    private static final Task[][] TASKS = {
        {
            new Task("fumaca", 4, 3, "Fumaça entre as copas",
                "Você percebe fumaça fina subindo perto das árvores. Fogo pequeno pode crescer depressa com vento e matéria seca; avisar cedo é uma forma de proteção."),
            new Task("folhas", 13, 4, "Folhas muito secas",
                "As folhas secas formam uma camada contínua no chão. Elas não causam incêndios sozinhas, mas facilitam que uma faísca se espalhe. O risco existe e precisa de cuidado."),
            new Task("fogueira", 19, 7, "Fogueira abandonada",
                "Há marcas de uma fogueira mal apagada. Você identifica o problema sem tocar nela: em uma situação real, a orientação é se afastar, avisar responsáveis e acionar os serviços locais."),
        },
        {
            new Task("lixo", 5, 10, "Resíduos na trilha",
                "Você recolhe resíduos leves para descarte correto. Além de poluir, vidro e lixo deixado em áreas naturais aumentam perigos e prejudicam animais."),
            new Task("placa", 11, 5, "Placa de prevenção caída",
                "A placa de orientação está no chão. Você a reposiciona para lembrar visitantes de não acender fogo e de levar seus resíduos embora."),
            new Task("atalho", 20, 4, "Atalho sobre vegetação",
                "Um atalho abriu o solo e esmagou mudas. Você sinaliza a área para que a trilha oficial seja usada: conservar também é evitar compactação e destruição desnecessária."),
        },
        {
            new Task("garrafas", 5, 4, "Garrafas perto da nascente",
                "As garrafas não pertencem à margem. Você as separa para descarte, preservando a água e reduzindo risco para os animais que bebem ali."),
            new Task("margem", 14, 10, "Margem exposta",
                "A vegetação da margem protege a água e o solo. Você registra o ponto para recuperação com espécies adequadas, em vez de arrancar ou plantar ao acaso."),
            new Task("vazamento", 20, 7, "Rastro de poluição",
                "Você encontra uma mancha suspeita longe do rio e marca sua localização. Contaminação deve ser comunicada a responsáveis e órgãos ambientais: não é seguro tentar resolver sem apoio."),
        },
        {
            new Task("alerta", 5, 7, "Alerta à comunidade",
                "Você aciona o aviso e indica uma rota segura. Antes de qualquer combate, pessoas e animais devem sair do caminho da fumaça e das chamas."),
            new Task("brigada", 11, 3, "Chamar a brigada",
                "Você transmite o local e a direção do vento para a brigada. Profissionais têm treinamento e equipamentos para avaliar como agir sem criar novas vítimas."),
            new Task("aceiro", 15, 6, "Preparar faixa de contenção",
                "Sob orientação da brigada, você ajuda a sinalizar uma faixa já planejada para interromper combustível vegetal. Não é uma atividade para improvisar: o planejamento protege todos."),
            new Task("fauna", 14, 12, "Proteger a passagem da fauna",
                "Você mantém livre a passagem para longe do foco, reduz ruído e acompanha a equipe. Animais não devem ser perseguidos: precisam de espaço e de uma rota segura."),
        }
    };

    private static final DialogueLine[][] INTRODUCTIONS = {
        {
            new DialogueLine("Narradora", "A Mata do Encanto acorda em cores vivas, mas o chão está seco e o vento muda de direção. Você chega para aprender a caminhar com respeito: observar, perguntar e cuidar antes que o problema cresça."),
            new DialogueLine("Saci", "Pulei de redemoinho em redemoinho e vi três sinais estranhos. Não quero que você corra atrás de chamas; quero que aprenda a reconhecer perigo. Procure fumaça, material muito seco e marcas de fogo deixadas por alguém."),
            new DialogueLine("Saci", "A primeira lição é simples: prevenção não é tarefa de uma pessoa só. Ao encontrar cada sinal, leia com atenção. Depois volte ao mapa e continue a investigação. Use E perto dos símbolos !."),
            new DialogueLine("Missão", "Capítulo I iniciado. Explore a floresta, encontre três sinais de risco e entenda por que cada um merece atenção. O Códice (C) traz mais informações sobre as lendas.")
        },
        {
            new DialogueLine("Caipora", "As trilhas são caminhos de encontro, não atalhos para ferir a mata. Quando alguém deixa lixo, ignora placas ou pisa onde não deve, o dano parece pequeno — até muitas pegadas transformarem o lugar."),
            new DialogueLine("Caipora", "Você já aprendeu a perceber o risco. Agora cuide de três pontos da trilha: resíduos, uma placa caída e um atalho que machuca as mudas. A proteção da floresta começa por escolhas repetidas todos os dias."),
            new DialogueLine("Missão", "Capítulo II iniciado. Ajude a Caipora a restaurar a trilha e observe como uma visita responsável reduz incêndios, erosão e perturbação da fauna.")
        },
        {
            new DialogueLine("Iara", "A água não termina onde nossos olhos deixam de vê-la. Nascentes, margens e igarapés sustentam pessoas, plantas e animais; aquilo que entra no solo pode alcançar muitos caminhos."),
            new DialogueLine("Iara", "Procure garrafas perto da nascente, uma margem exposta e um rastro de poluição. Não tente ser heroína ou herói sozinho diante de contaminação: identificar, registrar e comunicar também são atos de cuidado."),
            new DialogueLine("Missão", "Capítulo III iniciado. Proteja três pontos ligados à água e descubra por que cuidar das margens ajuda a manter a floresta viva.")
        },
        {
            new DialogueLine("Boitatá", "Meu brilho avisa que o fogo está perto. O fogo pode fazer parte de alguns ciclos naturais, mas queimadas provocadas ou fora de controle devastam vidas, solo, água e ar."),
            new DialogueLine("Boitatá", "Esta é a hora de agir com responsabilidade, não de brincar de coragem. Primeiro alerte a comunidade; depois chame a brigada. A equipe especializada definirá a contenção, e nós protegeremos a passagem da fauna."),
            new DialogueLine("Missão", "Capítulo IV iniciado. Complete as quatro ações de resposta. A missão só termina quando segurança das pessoas, trabalho profissional e proteção da vida caminham juntos.")
        }
    };

    private static final DialogueLine[][] CLOSINGS = {
        {
            new DialogueLine("Saci", "Muito bem. Você não apagou uma floresta com as próprias mãos — fez algo mais importante: reconheceu os sinais antes da emergência. Prevenir é agir cedo, compartilhar informação e não normalizar o perigo."),
            new DialogueLine("Narradora", "Ao registrar os riscos, você ajuda a comunidade a decidir com mais cuidado. Saci aponta uma trilha onde pegadas e resíduos contam outra história."),
        },
        {
            new DialogueLine("Caipora", "Trilha bem cuidada é trilha que respeita limites. Levar o próprio lixo, obedecer placas e não abrir atalhos protege solo, plantas e animais. Pequenas atitudes, quando repetidas, viram cuidado coletivo."),
            new DialogueLine("Narradora", "A mata se abre para uma área de água. De longe, um canto alerta que proteção da floresta também é proteção das nascentes."),
        },
        {
            new DialogueLine("Iara", "Água limpa depende de margens vivas e de escolhas responsáveis. Ao perceber poluição, a melhor decisão é evitar contato e avisar quem pode avaliar. Cuidar não significa assumir risco desnecessário."),
            new DialogueLine("Narradora", "Uma fumaça mais densa corta o horizonte. A investigação feita nos capítulos anteriores agora ajuda a agir com serenidade e segurança."),
        },
        {
            new DialogueLine("Boitatá", "A resposta segura uniu informação, brigada, contenção planejada e respeito aos animais. Que esta aventura atravesse a tela: prevenir queimadas, proteger a água e valorizar os saberes populares é cuidar de um território comum."),
            new DialogueLine("Narradora", "A Mata do Encanto continua viva porque muitas pessoas escolheram escutar, aprender e agir juntas. As lendas não ficam presas ao passado: elas inspiram novas formas de guardar a floresta."),
        }
    };

    private static final FolkloreEntry[] CODEX = {
        new FolkloreEntry("Saci", "O guardião das travessuras", "Figura presente em muitas versões do folclore brasileiro, o Saci é frequentemente lembrado por sua perna única, gorro vermelho e redemoinhos. Nesta história ele transforma curiosidade em atenção: antes de agir, observe os sinais e compartilhe o alerta.", 0xE74C3C),
        new FolkloreEntry("Caipora", "Protetora dos caminhos", "A Caipora aparece em tradições de diferentes regiões como defensora da mata e dos animais. O jogo a relaciona ao uso responsável das trilhas: respeitar sinalizações e não deixar resíduos ajuda a manter o ambiente seguro para toda forma de vida.", 0xF39C12),
        new FolkloreEntry("Iara", "A voz das águas", "Iara é uma personagem ligada a rios e encantamentos em narrativas brasileiras. Aqui, ela convida a perceber que nascentes e margens são interligadas. Água limpa depende de vegetação, descarte correto e comunicação de problemas ambientais.", 0x3498DB),
        new FolkloreEntry("Boitatá", "A luz que alerta", "O Boitatá é conhecido como uma serpente de fogo ou luz em diversas narrativas. No jogo, seu brilho não romantiza a queimada: é um aviso para buscar ajuda especializada, proteger pessoas e fauna e evitar ações improvisadas.", 0xF1C40F),
        new FolkloreEntry("Cuidado coletivo", "Aprendizado que segue fora da tela", "Prevenir queimadas inclui não acender fogueiras em áreas de risco, não descartar lixo na natureza, respeitar orientações locais e avisar autoridades ou brigadas diante de fumaça e fogo. Em emergências reais, priorize sua segurança e siga serviços oficiais.", 0x2ECC71)
    };

    private final Set<String> completed = new HashSet<String>();
    private int chapter;
    private int playerX;
    private int playerY;

    GameSession() { reset(); }

    void reset() {
        chapter = 0;
        completed.clear();
        playerX = 2;
        playerY = 12;
    }

    int chapter() { return chapter; }
    String chapterName() { return CHAPTER_NAMES[chapter]; }
    String guardian() { return GUARDIANS[chapter]; }
    String objective() { return OBJECTIVES[chapter]; }
    int playerX() { return playerX; }
    int playerY() { return playerY; }
    void movePlayer(int dx, int dy) { playerX += dx; playerY += dy; }

    boolean isBlocked(int x, int y) {
        if (x <= 0 || x >= 23 || y <= 0 || y >= 14) return true;
        if (x == 7 && y >= 3 && y <= 7) return true;
        if (x == 12 && y >= 8 && y <= 12) return true;
        if (x == 4 && y >= 6 && y <= 8) return true;
        if (x == 17 && y >= 2 && y <= 4) return true;
        if (x >= 16 && x <= 22 && y >= 9 && y <= 12 && !(x == 19 && y == 11)) return true;
        return false;
    }

    List<Task> remainingTasks() {
        List<Task> remaining = new ArrayList<Task>();
        for (Task task : TASKS[chapter]) if (!completed.contains(task.id)) remaining.add(task);
        return remaining;
    }

    Task nearbyTask() {
        for (Task task : remainingTasks()) {
            if (Math.abs(task.x - playerX) + Math.abs(task.y - playerY) <= 1) return task;
        }
        return null;
    }

    boolean complete(Task task) {
        completed.add(task.id);
        return remainingTasks().isEmpty();
    }

    void nextChapter() {
        chapter++;
        playerX = 2;
        playerY = 12;
    }

    DialogueLine[] introduction() { return INTRODUCTIONS[chapter]; }
    DialogueLine[] closing() { return CLOSINGS[chapter]; }
    FolkloreEntry[] codex() { return Arrays.copyOf(CODEX, CODEX.length); }

    DialogueLine[] taskFeedback(Task task) {
        String count = "Progresso: " + (TASKS[chapter].length - remainingTasks().size()) + "/" + TASKS[chapter].length + " ações concluídas.";
        return new DialogueLine[] {
            new DialogueLine("Descoberta", task.label),
            new DialogueLine("Feedback", task.detail),
            new DialogueLine("Missão", count)
        };
    }
}
