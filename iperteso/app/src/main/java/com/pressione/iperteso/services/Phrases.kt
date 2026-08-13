package com.pressione.iperteso.services

import com.pressione.iperteso.domain.model.Category
import kotlin.random.Random

/**
 * Ironic phrases shown after saving a reading, ported from the web app's phrases.js.
 * Mapped by ESC/ESH category. Gendered variants resolve to masculine by default.
 * ~30% chance of no phrase; never repeats the last phrase for the same category.
 */
object Phrases {

    private data class G(val m: String, val f: String = m)

    private enum class Pool { NORMAL, ELEVATED, STAGE_1, STAGE_2, CRISIS }

    private val POOLS: Map<Pool, List<G>> = mapOf(
        Pool.NORMAL to listOf(
            G("Pressione da manuale. Praticamente sei un monaco buddista in vacanza.", "Pressione da manuale. Praticamente sei una monaca buddista in vacanza."),
            G("Tutto perfetto! Il tuo cuore batte al ritmo di una ballata rilassante."),
            G("Valori così calmi che persino il tuo divano è geloso di tanta stabilità."),
            G("Livello di zen: massimo. Puoi affrontare la riunione del lunedì senza paura."),
            G("Valori così perfetti che potresti fare il collaudatore di amache a livello professionale.", "Valori così perfetti che potresti fare la collaudatrice di amache a livello professionale."),
            G("Questa non è pressione: è una recensione a cinque stelle del tuo sistema cardiovascolare."),
            G("Il misuratore ha guardato il risultato e ha detto: «Va bene, non servo più»."),
            G("Pressione ottimale. Il cuore oggi è praticamente in smart working."),
            G("Numeri così belli che quasi viene voglia di incorniciarli."),
            G("Il tuo sangue oggi viaggia in prima classe, senza ritardi e senza coincidenze."),
            G("Tutto perfetto. Persino il misuratore può andare in pausa caffè."),
            G("Complimenti: oggi il tuo cuore non ha assolutamente voglia di fare straordinari."),
            G("Perfetta: oggi il tuo cuore lavora con contratto regolare."),
            G("La pressione è normale. Finalmente una cosa nella tua vita che non ha bisogno di essere discussa."),
            G("Il misuratore ha detto sì. Il tuo cuore può tornare alle sue attività."),
            G("Ottimo risultato: nessun drama cardiovascolare per oggi."),
            G("La pressione è tranquilla. Praticamente l'unico membro della famiglia che non crea problemi."),
            G("Tutto nella norma: puoi smettere di fissare quel numerino come se fosse l'esito di Sanremo."),
            G("Il cuore oggi ha scelto la diplomazia.")
        ),
        Pool.ELEVATED to listOf(
            G("Valori leggermente frizzanti. Qualcuno ha abusato del caffè stamattina?"),
            G("Un po' su di giri. Forse è colpa del traffico o di quel messaggio non letto."),
            G("La pressione sale un briciolo. Fai tre respiri profondi e ignora i parenti."),
            G("Un po' altina. È il momento ideale per sedersi e delegare i problemi altrui."),
            G("Sei nella zona «non facciamo drammi, ma diamoci un'occhiata»."),
            G("La pressione non è alta: è semplicemente in anticipo sull'ansia."),
            G("Il cuore oggi ha detto: «Facciamo un pochino più forte, così ci divertiamo»."),
            G("Sei praticamente al confine: la pressione sta facendo turismo."),
            G("Il misuratore ti ha dato un piccolo spoiler: rilassati un attimo."),
            G("Non è una bocciatura, è un «potresti fare meglio» scritto in millimetri di mercurio."),
            G("La pressione è nella categoria «bravo, però non montarti la testa».")
        ),
        Pool.STAGE_1 to listOf(
            G("Ok, il motore è caldo. Forse è il caso di scalare una marcia e rallentare."),
            G("La tua pressione sta cercando di scalare l'Everest. Rimandala a valle."),
            G("Valori vivaci. Respira, conta fino a dieci e ricordati che non sei un supereroe.", "Valori vivaci. Respira, conta fino a dieci e ricordati che non sei una supereroina."),
            G("Zona arancione! Metti giù quel sale e allontani la persona che ti sta stressando."),
            G("Pressione in modalità 'pentola a pressione'. Comincia a far uscire un po' di vapore."),
            G("Sei leggermente su di giri. Hai per caso incrociato lo sguardo del tuo capo?"),
            G("I numeri salgono. È il momento di attivare la modalità aereo nella tua testa."),
            G("Valori frizzanti. Meno caffeina, meno drammi e più respiri profondi, grazie."),
            G("La pressione è un po' alta. Evidentemente anche lei oggi ha delle aspettative."),
            G("Il tuo cuore ha messo la modalità «lavoro straordinario» senza chiedere il permesso."),
            G("Non preoccuparti: non sei agitato. È il tuo sangue che ha fretta."),
            G("Il misuratore ti sta suggerendo, con molta diplomazia, di prenderti una pausa."),
            G("La pressione è salita un pochino. Evidentemente ha ricevuto una buona notizia prima di noi."),
            G("Direi che il tuo sistema cardiovascolare oggi è leggermente troppo motivato."),
            G("Niente panico: al massimo il tuo sangue sta facendo un po' di networking."),
            G("La pressione è un po' alta: qualcuno qui ha bevuto il caffè anche col sangue."),
            G("Il tuo cuore oggi ha deciso di non prendere l'ascensore."),
            G("Sei leggermente sopra il limite: praticamente la pressione ha parcheggiato sulle strisce."),
            G("Il sangue oggi ha fretta. Non sappiamo dove debba andare, ma sicuramente è in ritardo."),
            G("La pressione è salita di livello, ma almeno non ha ancora sbloccato il boss finale."),
            G("Il tuo cuore sta facendo gli straordinari, ma senza aver compilato il modulo ferie."),
            G("Il misuratore ti sta dicendo: «Guarda che possiamo anche prendercela con calma»."),
            G("La pressione è un po' alta. Evidentemente anche le tue arterie hanno una giornata no."),
            G("Non è preoccupante, è solo il tuo sistema cardiovascolare che oggi vuole attirare l'attenzione."),
            G("Il cuore ha messo la modalità «facciamo una cosa veloce» e poi non si è più fermato."),
            G("Sei appena entrato nella categoria «signore, abbassi i toni»."),
            G("La pressione è sopra la media: anche il tuo sangue vuole distinguersi."),
            G("Il tuo apparato cardiovascolare oggi ha scelto il capitalismo: massima produttività."),
            G("Il misuratore ha visto il risultato e ha detto: «Interessante. Ne parliamo con calma.»"),
            G("La pressione è salita, ma almeno non ha ancora chiesto di essere chiamata «eccellenza»."),
            G("Oggi il tuo sangue non è fluido: è un pendolare romano alle 8 di mattina.")
        ),
        Pool.STAGE_2 to listOf(
            G("Calma! Il tuo cuore sta correndo la Formula 1 ma tu sei ancora sul divano."),
            G("Decisamente alta. Fai finta di essere un bradipo per i prossimi venti minuti."),
            G("Valori bollenti. È il segnale ufficiale per smettere di fare qualsiasi cosa."),
            G("Il display è rosso. Diventa imperativo rilassarsi prima di trasformarsi in Hulk."),
            G("Il tuo cuore sta battendo i record di velocità di Hamilton. Peccato che tu sia seduto.", "Il tuo cuore sta battendo i record di velocità di Hamilton. Peccato che tu sia seduta."),
            G("Sei a un passo dal trasformarti in un vulcano in eruzione. Trova un posto fresco e rilassati."),
            G("La pressione è così alta che potresti gonfiare i pneumatici dell'auto. Fermati un attimo!"),
            G("Valori decisamente 'strong'. Metti giù il telecomando, chiudi gli occhi e fai finta di essere un sasso."),
            G("La pressione non è alta: sta semplicemente cercando di vedere meglio il panorama."),
            G("Complimenti, hai appena trasformato il braccio in una centrale idroelettrica."),
            G("Questa non è pressione: è ambizione. Forse un po' troppa."),
            G("Il misuratore ha visto il numero e ha chiesto: «Sicuro di voler continuare?»"),
            G("Direi che oggi il tuo sangue ha deciso di fare le scale di corsa."),
            G("Tranquillo, non è un risultato: è un grido d'aiuto con i numeri."),
            G("A questo punto non misuriamo più la pressione, chiediamo direttamente il meteo."),
            G("La pressione è talmente alta che il misuratore ha chiesto un aumento di stipendio."),
            G("Il tuo cuore oggi non pompa: fa il DJ. E ha messo tutto a palla."),
            G("Questi numeri non sono una pressione, sono una minaccia scritta in formato digitale."),
            G("La pressione è salita così tanto che adesso paga l'affitto al piano di sopra."),
            G("Il tuo sangue evidentemente ha prenotato un volo e ha paura di perderlo."),
            G("Il cuore oggi: «Ragazzi, acceleriamo che c'è traffico!»"),
            G("Non hai la pressione alta, hai semplicemente un sistema cardiovascolare molto competitivo."),
            G("Il misuratore ha fatto il conto e ha detto: «Io su questo non mi esprimo»."),
            G("La pressione è così alta che tra poco serve il nulla osta dell'ENAC."),
            G("Il tuo cuore sta lavorando come se avesse scoperto che chiude il supermercato tra cinque minuti."),
            G("Hai una pressione che non entra più nei parametri: vuole direttamente diventare amministratore delegato."),
            G("Il sangue oggi non circola: corre dietro ai suoi sogni."),
            G("A questi livelli il misuratore non misura più: giudica."),
            G("Il tuo cuore ha confuso «ritmo sostenuto» con «finale olimpica»."),
            G("Questa pressione ha più grinta di te il lunedì mattina."),
            G("Il misuratore: «Riproviamo?» Tu: «Perché, vuoi farmi arrabbiare ancora di più?»")
        ),
        Pool.CRISIS to listOf(
            G("Se fossi una pentola a pressione, faresti fischiare anche i vicini. Ti prego, rilassati subito!"),
            G("Valori da record, ma di quelli che non vogliamo premiare. Mettiti comodo e avvisa un medico.", "Valori da record, ma di quelli che non vogliamo premiare. Mettiti comoda e avvisa un medico."),
            G("Il tuo cuore sta facendo un concerto heavy metal. È il momento di chiamare i soccorsi per sicurezza."),
            G("Allarme rosso scuro. Non è uno scherzo: siediti, respira e contatta subito il dottore."),
            G("I sensori stanno ballando il samba. Non ignorare questo numero: chiama subito il medico."),
            G("Ok, spegni tutto. Il tuo sistema è in surriscaldamento globale. Contatta il dottore adesso."),
            G("Valori da codice rosso fisso. Niente panico, ma siediti, respira e fatti dare un'occhiata da un professionista."),
            G("Il cuore sta esagerando con gli effetti speciali. Mettiti comodo e chiama il medico per sicurezza.", "Il cuore sta esagerando con gli effetti speciali. Mettiti comoda e chiama il medico per sicurezza.")
        )
    )

    private val lastIndex = mutableMapOf<Category, Int>()

    private fun Category.toPool(): Pool = when (this) {
        Category.OPTIMAL, Category.NORMAL -> Pool.NORMAL
        Category.HIGH_NORMAL -> Pool.ELEVATED
        Category.GRADE_1 -> Pool.STAGE_1
        Category.GRADE_2, Category.GRADE_3 -> Pool.STAGE_2
        Category.CRISIS -> Pool.CRISIS
    }

    /**
     * Pick a random phrase for the given category, or null (~30% of the time).
     */
    fun getRandomPhrase(category: Category, gender: String?): String? {
        val pool = POOLS[category.toPool()] ?: return null
        if (pool.isEmpty()) return null
        if (Random.nextFloat() < 0.30f) return null

        var eligible = pool.indices.toList()
        val last = lastIndex[category]
        if (eligible.size > 1 && last != null && last in pool.indices) {
            eligible = eligible.filter { it != last }
        }

        val idx = eligible[Random.nextInt(eligible.size)]
        lastIndex[category] = idx
        val entry = pool[idx]
        return if (gender == "female") entry.f else entry.m
    }
}

/**
 * Holds the last phrase to show on Home after saving a reading.
 */
object PhraseStore {
    var pending: String? = null
}
