package fr.loupgarou.app

import fr.loupgarou.engine.Camp
import fr.loupgarou.engine.Role

/** Une réplique du narrateur : le texte affiché et l'enregistrement de la voix. */
data class Replique(val texte: String, val son: Int)

/** Banque de répliques, générée depuis le script de la voix (voix/script-gemini.md). */
object Repliques {
    val endort = listOf(
        Replique("C'est la nuit. Le village s'endort. Fermez les yeux.", R.raw.v01_01),
        Replique("La nuit tombe sur Thiercelieux. Tout le monde ferme les yeux… et prie.", R.raw.v01_02),
        Replique("Le soleil se couche. Fermez les yeux, et tâchez de les rouvrir demain.", R.raw.v01_03),
        Replique("Le village s'endort. Certains plus profondément que d'autres.", R.raw.v01_04),
        Replique("C'est l'heure où les honnêtes gens dorment. Les autres, patience.", R.raw.v01_05),
        Replique("La lune se lève sur Thiercelieux. Fermez les yeux, et ne trichez pas.", R.raw.v01_06),
        Replique("Bonne nuit, braves gens. Pour certains, ce sera la dernière.", R.raw.v01_07),
        Replique("Le village s'endort, bercé par des bruits de pas qu'il vaut mieux ne pas entendre.", R.raw.v01_08),
    )
    val voleur = listOf(
        Replique("Le Voleur se réveille. Deux cartes l'attendent au centre. Sers-toi, personne ne regarde.", R.raw.v02_01),
        Replique("Voleur, ouvre les yeux. Les poches vides, la conscience aussi.", R.raw.v02_02),
        Replique("Le Voleur s'éveille. Ce soir, tu peux changer de vie. Littéralement.", R.raw.v02_03),
        Replique("Voleur, réveille-toi. Une identité de rechange, ça ne se refuse pas.", R.raw.v02_04),
        Replique("Le Voleur ouvre les yeux. Choisis bien : on ne rend pas la marchandise.", R.raw.v02_05),
        Replique("Voleur, c'est ton moment. Discrètement, comme d'habitude.", R.raw.v02_06),
    )
    val cupidon = listOf(
        Replique("Cupidon se réveille. Choisis deux cœurs à lier, jusqu'à ce que la mort les sépare. Ensemble.", R.raw.v03_01),
        Replique("Cupidon, ouvre les yeux. Ce soir, tu fais deux heureux. Provisoirement.", R.raw.v03_02),
        Replique("Cupidon s'éveille. Tends ton arc, et vise bien : l'amour, ici, c'est à la vie, à la mort.", R.raw.v03_03),
        Replique("Cupidon, réveille-toi. Deux âmes vont tomber amoureuses. Et peut-être tomber tout court.", R.raw.v03_04),
        Replique("Cupidon ouvre les yeux. Choisis deux amoureux. Toi-même, si tu es d'humeur romantique.", R.raw.v03_05),
        Replique("Cupidon, à toi. Unis deux destins, ils ne pourront plus se défaire.", R.raw.v03_06),
    )
    val amoureux = listOf(
        Replique("Les Amoureux se réveillent et se découvrent. Profitez-en, ça ne durera peut-être pas.", R.raw.v04_01),
        Replique("Amoureux, ouvrez les yeux et regardez-vous. Désormais, si l'un tombe, l'autre suit.", R.raw.v04_02),
        Replique("Les Amoureux s'éveillent. Un regard suffit. C'est beau. C'est fragile.", R.raw.v04_03),
        Replique("Amoureux, réveillez-vous et contemplez votre moitié. Votre moitié de cercueil, peut-être.", R.raw.v04_04),
        Replique("Les Amoureux ouvrent les yeux. L'amour rend aveugle. Ici, il rend surtout mortel.", R.raw.v04_05),
        Replique("Amoureux, découvrez-vous. Et rendormez-vous, main dans la main.", R.raw.v04_06),
    )
    val voyante = listOf(
        Replique("La Voyante se réveille. Choisis l'âme que tu veux sonder.", R.raw.v05_01),
        Replique("Voyante, ouvre les yeux. Ta boule de cristal a faim de vérité.", R.raw.v05_02),
        Replique("La Voyante s'éveille. Qui cache quoi, ce soir ?", R.raw.v05_03),
        Replique("Voyante, réveille-toi. Un seul secret par nuit, choisis-le bien.", R.raw.v05_04),
        Replique("La Voyante ouvre les yeux. Les masques tombent, mais un seul à la fois.", R.raw.v05_05),
        Replique("Voyante, c'est ton heure. Regarde au fond d'une âme. Ne crie pas.", R.raw.v05_06),
        Replique("La Voyante s'éveille. Le village ment, toi tu sais. Ou presque.", R.raw.v05_07),
        Replique("Voyante, ouvre les yeux. Que la vérité soit avec toi. Elle n'est avec personne d'autre.", R.raw.v05_08),
    )
    val loups = listOf(
        Replique("Les Loups-Garous se réveillent. Le dîner est servi, il ne reste qu'à choisir le plat.", R.raw.v06_01),
        Replique("Loups-Garous, ouvrez les yeux. Le village dort, et il sent bon.", R.raw.v06_02),
        Replique("Les Loups-Garous s'éveillent. Concertez-vous en silence : qui ne verra pas l'aube ?", R.raw.v06_03),
        Replique("Loups-Garous, réveillez-vous. La faim vous tiraille. Le village, lui, ronfle.", R.raw.v06_04),
        Replique("Les Loups-Garous ouvrent les yeux. Choisissez votre victime. Sans faire de miettes.", R.raw.v06_05),
        Replique("Loups-Garous, c'est l'heure du repas. Et ce soir, c'est un voisin.", R.raw.v06_06),
        Replique("Les Loups-Garous se réveillent. Babines retroussées, crocs affûtés : à table.", R.raw.v06_07),
        Replique("Loups-Garous, ouvrez les yeux. Désignez celui qui ne participera plus aux débats.", R.raw.v06_08),
        Replique("Les Loups-Garous s'éveillent. Un village entier à portée de crocs. L'embarras du choix.", R.raw.v06_09),
        Replique("Loups-Garous, réveillez-vous. Silence, on mange.", R.raw.v06_10),
    )
    val sorciere = listOf(
        Replique("La Sorcière se réveille. Deux fioles, un cadavre tout frais : à toi de voir.", R.raw.v07_01),
        Replique("Sorcière, ouvre les yeux. Sauver, empoisonner, ou retourner te coucher ?", R.raw.v07_02),
        Replique("La Sorcière s'éveille. Je te montre la victime des loups. Ta potion va-t-elle servir ?", R.raw.v07_03),
        Replique("Sorcière, réveille-toi. Ce soir, tu joues à la médecin. Ou à l'empoisonneuse.", R.raw.v07_04),
        Replique("La Sorcière ouvre les yeux. Une potion pour la vie, une pour la mort. Le chaudron bouillonne.", R.raw.v07_05),
        Replique("Sorcière, à toi. Le destin de quelqu'un tient dans une petite fiole.", R.raw.v07_06),
        Replique("La Sorcière s'éveille. Les loups ont fait leur choix. À toi de faire le tien.", R.raw.v07_07),
        Replique("Sorcière, ouvre les yeux. Remède ou poison, la nuit est longue.", R.raw.v07_08),
    )
    val rendormir = listOf(
        Replique("Rendors-toi, et repose l'appareil au centre.", R.raw.v08_01),
        Replique("Ferme les yeux. Repose l'appareil sans bruit.", R.raw.v08_02),
        Replique("C'est fait. Rendors-toi, comme si de rien n'était.", R.raw.v08_03),
        Replique("Retourne dans tes rêves. Ou tes cauchemars.", R.raw.v08_04),
        Replique("Ferme les yeux. Personne n'a rien vu. Normalement.", R.raw.v08_05),
        Replique("Rendormez-vous. La nuit continue sans vous.", R.raw.v08_06),
        Replique("Les yeux fermés, l'air innocent. Parfait.", R.raw.v08_07),
        Replique("Rendors-toi. Demain, il faudra mentir avec aplomb.", R.raw.v08_08),
    )
    val matinMorts = listOf(
        Replique("Le village se réveille. Enfin, presque tout le village.", R.raw.v09_01),
        Replique("Le soleil se lève sur une scène que personne n'oubliera. Surtout pas la victime.", R.raw.v09_02),
        Replique("Ce matin, le village pleure l'un des siens. Ou plusieurs.", R.raw.v09_03),
        Replique("Le jour se lève. Ouvrez les yeux, et comptez-vous.", R.raw.v09_04),
        Replique("Le coq chante. Pas pour tout le monde, hélas.", R.raw.v09_05),
        Replique("Le village s'éveille, et découvre qu'il manque du monde au petit-déjeuner.", R.raw.v09_06),
        Replique("C'est le matin. Une chaise reste vide. Peut-être deux.", R.raw.v09_07),
        Replique("Le soleil se lève sur Thiercelieux. La nuit a laissé des traces. Et des restes.", R.raw.v09_08),
        Replique("Réveillez-vous. Enfin, ceux qui le peuvent encore.", R.raw.v09_09),
        Replique("Le jour se lève, et avec lui, une odeur qui ne trompe pas.", R.raw.v09_10),
    )
    val matinPersonne = listOf(
        Replique("Le village se réveille… au complet. Les loups ont eu une nuit difficile.", R.raw.v10_01),
        Replique("Le soleil se lève et, miracle, personne ne manque à l'appel.", R.raw.v10_02),
        Replique("C'est le matin. Tout le monde est là. Étrange… et un peu décevant.", R.raw.v10_03),
        Replique("Réveillez-vous. Personne n'est mort cette nuit. Ne vous habituez pas.", R.raw.v10_04),
        Replique("Le village s'éveille, entier. Quelqu'un, quelque part, a été bien inspiré.", R.raw.v10_05),
        Replique("Le jour se lève sans cadavre. Les loups ont jeûné. Ils seront d'autant plus affamés.", R.raw.v10_06),
        Replique("Ouvrez les yeux. Pas une goutte de sang. Pour l'instant.", R.raw.v10_07),
        Replique("Le coq chante pour tout le monde, ce matin. Profitez-en.", R.raw.v10_08),
    )
    val electionCapitaine = listOf(
        Replique("Avant toute chose, le village doit se choisir un Capitaine. Sa voix comptera double.", R.raw.v11_01),
        Replique("Il est temps d'élire un Capitaine. Choisissez bien, il aura le dernier mot.", R.raw.v11_02),
        Replique("Le village a besoin d'un chef. Votez pour votre Capitaine. On ne peut pas refuser l'honneur.", R.raw.v11_03),
        Replique("Élisez votre Capitaine. Un grand pouvoir, et une jolie cible dans le dos.", R.raw.v11_04),
        Replique("Qui sera le Capitaine de Thiercelieux ? Votez à main levée.", R.raw.v11_05),
    )
    val successionCapitaine = listOf(
        Replique("Le Capitaine est tombé. Dans son dernier souffle, il désigne son successeur.", R.raw.v12_01),
        Replique("Le Capitaine nous quitte. Avant de partir, il transmet son écharpe.", R.raw.v12_02),
        Replique("Le Capitaine n'est plus. Qu'il choisisse qui reprendra le flambeau.", R.raw.v12_03),
        Replique("Mort, mais pas sans héritier. Capitaine, désigne ton successeur.", R.raw.v12_04),
        Replique("Le Capitaine s'éteint. Son dernier geste : nommer celui qui lui succède.", R.raw.v12_05),
    )
    val debutVote = listOf(
        Replique("Le village doit maintenant désigner un coupable. Débattez.", R.raw.v13_01),
        Replique("Place au débat. Accusez, défendez-vous, mentez. Puis votez.", R.raw.v13_02),
        Replique("Il est temps de trouver le loup parmi vous. Ou au moins quelqu'un qui y ressemble.", R.raw.v13_03),
        Replique("Le tribunal du village est ouvert. Que les accusations commencent.", R.raw.v13_04),
        Replique("Qui mérite la corde, aujourd'hui ? Discutez, puis votez à main levée.", R.raw.v13_05),
        Replique("Un loup se cache parmi vous. Démasquez-le avant ce soir.", R.raw.v13_06),
    )
    val relanceDebat = listOf(
        Replique("Le temps presse. La nuit approche.", R.raw.v14_01),
        Replique("Allons, allons. Les loups, eux, ont déjà fait leur choix.", R.raw.v14_02),
        Replique("Vous parlez beaucoup. Les loups, eux, écoutent.", R.raw.v14_03),
        Replique("Il faudra bien que quelqu'un y passe.", R.raw.v14_04),
        Replique("Plus qu'une minute. Pensez à votre dernier mot.", R.raw.v14_05),
        Replique("Certains sont bien silencieux. C'est suspect, le silence.", R.raw.v14_06),
        Replique("Le soleil décline. Décidez-vous.", R.raw.v14_07),
        Replique("Un innocent ou un loup, la corde ne fait pas la différence.", R.raw.v14_08),
    )
    val condamne = listOf(
        Replique("Le village a parlé. La corde aussi.", R.raw.v16_01),
        Replique("La sentence tombe. Le condamné aussi.", R.raw.v16_02),
        Replique("Le village a choisi. Espérons qu'il ne s'est pas trompé.", R.raw.v16_03),
        Replique("Justice est faite. Ou pas. Retournez sa carte.", R.raw.v16_04),
        Replique("Le verdict est sans appel. Adieu, voisin.", R.raw.v16_05),
        Replique("La foule a tranché. Voyons qui vous venez de pendre.", R.raw.v16_06),
        Replique("C'est la démocratie. Elle est parfois un peu brutale.", R.raw.v16_07),
        Replique("Le village exécute sa décision. Et son voisin.", R.raw.v16_08),
    )
    val chagrin = listOf(
        Replique("Un cœur se brise. Son Amoureux le suit dans la tombe.", R.raw.v18_01),
        Replique("L'amour était plus fort que tout. Même que la vie.", R.raw.v18_02),
        Replique("Inconsolable, l'Amoureux s'éteint à son tour.", R.raw.v18_03),
        Replique("Ils s'étaient juré de ne jamais se quitter. Promesse tenue.", R.raw.v18_04),
        Replique("Le chagrin emporte l'autre Amoureux. C'était beau, c'était tragique.", R.raw.v18_05),
    )
    val egalite = listOf(
        Replique("Égalité. Le village hésite.", R.raw.v15_01),
        Replique("Le village est divisé. Il faut départager les ex-æquo.", R.raw.v15_03),
        Replique("Égalité. Revotez entre les accusés.", R.raw.v15_04),
        Replique("Personne ne se décide. Il va falloir choisir, pourtant.", R.raw.v15_05),
    )
    val departageCapitaine = listOf(
        Replique("Égalité parfaite. Le Capitaine va trancher.", R.raw.v15_02),
    )
    val chasseur = listOf(
        Replique("Le Chasseur est touché. Mais il lui reste une balle.", R.raw.v17_01),
        Replique("Le Chasseur tombe, et dans un dernier sursaut, il épaule son fusil.", R.raw.v17_02),
        Replique("Un Chasseur ne part jamais seul. Désigne ta cible.", R.raw.v17_03),
        Replique("Le Chasseur rend son dernier souffle, et presse la détente.", R.raw.v17_04),
        Replique("Le Chasseur s'effondre. Quelqu'un va l'accompagner.", R.raw.v17_05),
    )
    val tirDuChasseur = listOf(
        Replique("Le Chasseur a tiré. Il ne part pas seul.", R.raw.v17_06),
    )
    val revelation: Map<Role, List<Replique>> = mapOf(
        Role.LOUP_GAROU to listOf(Replique("C'était un Loup-Garou ! Un de moins.", R.raw.v19_01), Replique("Sous la peau, des poils. C'était un Loup-Garou.", R.raw.v19_02)),
        Role.VILLAGEOIS to listOf(Replique("C'était un Simple Villageois. Innocent, et désormais inutile.", R.raw.v19_03), Replique("Un Simple Villageois. Oups.", R.raw.v19_04)),
        Role.VOYANTE to listOf(Replique("C'était la Voyante. Elle n'avait pas vu venir celle-là.", R.raw.v19_05), Replique("La Voyante nous quitte. Ses secrets aussi.", R.raw.v19_06)),
        Role.CHASSEUR to listOf(Replique("C'était le Chasseur. Il avait toujours son fusil, en tout cas.", R.raw.v19_07), Replique("Le Chasseur est mort. Le gibier respire.", R.raw.v19_08)),
        Role.CUPIDON to listOf(Replique("C'était Cupidon. L'amour a perdu son meilleur archer.", R.raw.v19_09), Replique("Cupidon est mort. Ses flèches, elles, restent plantées.", R.raw.v19_10)),
        Role.SORCIERE to listOf(Replique("C'était la Sorcière. Plus de potions, plus de miracles.", R.raw.v19_11), Replique("La Sorcière est morte. Son chaudron refroidit.", R.raw.v19_12)),
        Role.VOLEUR to listOf(Replique("C'était le Voleur. Il ne volera plus rien, pas même une seconde de plus.", R.raw.v19_13), Replique("Le Voleur est mort. On vérifiera ses poches.", R.raw.v19_14)),
        Role.PETITE_FILLE to listOf(Replique("C'était la Petite fille. Elle aurait dû garder les yeux fermés.", R.raw.v19_15), Replique("La Petite fille nous quitte. Trop curieuse, sans doute.", R.raw.v19_16)),
    )
    val fin: Map<Camp, List<Replique>> = mapOf(
        Camp.VILLAGEOIS to listOf(Replique("Les Villageois l'emportent ! Le dernier Loup-Garou a rejoint ses victimes.", R.raw.v20_01), Replique("Victoire du village ! Thiercelieux peut enfin dormir sur ses deux oreilles.", R.raw.v20_02)),
        Camp.LOUPS to listOf(Replique("Les Loups-Garous l'emportent. Le village est calme, désormais. Très calme.", R.raw.v20_03), Replique("Victoire des Loups-Garous ! Il ne reste plus personne à manger. Dommage.", R.raw.v20_04)),
        Camp.AMOUREUX to listOf(Replique("Les Amoureux l'emportent, seuls au monde. Ils l'ont bien cherché.", R.raw.v20_05), Replique("Victoire des Amoureux ! Contre tous, envers et contre tout. Et sur un tas de cadavres.", R.raw.v20_06)),
        Camp.PERSONNE to listOf(Replique("Plus personne. Le village est désert, et les corbeaux ont gagné.", R.raw.v20_07)),
    )
}
