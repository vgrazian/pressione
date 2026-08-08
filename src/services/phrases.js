/**
 * Ironic phrases shown after saving a reading.
 * Mapped by ReadingCategory key. Sometimes no phrase is shown (~30% chance).
 */

const PHRASES = {
    NORMAL: [
        "Pressione da manuale. Praticamente sei un monaco buddista in vacanza.",
        "Tutto perfetto! Il tuo cuore batte al ritmo di una ballata rilassante.",
        "Valori così calmi che persino il tuo divano è geloso di tanta stabilità.",
        "Livello di zen: massimo. Puoi affrontare la riunione del lunedì senza paura.",
        "Valori così perfetti che potresti fare il collaudatore di amache a livello professionale."
    ],
    ELEVATED: [
        "Valori leggermente frizzanti. Qualcuno ha abusato del caffè stamattina?",
        "Un po' su di giri. Forse è colpa del traffico o di quel messaggio non letto.",
        "La pressione sale un briciolo. Fai tre respiri profondi e ignora i parenti.",
        "Un po' altina. È il momento ideale per sedersi e delegare i problemi altrui."
    ],
    HYPERTENSION_STAGE_1: [
        "Ok, il motore è caldo. Forse è il caso di scalare una marcia e rallentare.",
        "La tua pressione sta cercando di scalare l'Everest. Rimandala a valle.",
        "Valori vivaci. Respira, conta fino a dieci e ricordati che non sei un supereroe.",
        "Zona arancione! Metti giù quel sale e allontani la persona che ti sta stressando.",
        "Pressione in modalità 'pentola a pressione'. Comincia a far uscire un po' di vapore.",
        "Sei leggermente su di giri. Hai per caso incrociato lo sguardo del tuo capo?",
        "I numeri salgono. È il momento di attivare la modalità aereo nella tua testa.",
        "Valori frizzanti. Meno caffeina, meno drammi e più respiri profondi, grazie."
    ],
    HYPERTENSION_STAGE_2: [
        "Calma! Il tuo cuore sta correndo la Formula 1 ma tu sei ancora sul divano.",
        "Decisamente alta. Fai finta di essere un bradipo per i prossimi venti minuti.",
        "Valori bollenti. È il segnale ufficiale per smettere di fare qualsiasi cosa.",
        "Il display è rosso. Diventa imperativo rilassarsi prima di trasformarsi in Hulk.",
        "Il tuo cuore sta battendo i record di velocità di Hamilton. Peccato che tu sia seduto.",
        "Sei a un passo dal trasformarti in un vulcano in eruzione. Trova un posto fresco e rilassati.",
        "La pressione è così alta che potresti gonfiare i pneumatici dell'auto. Fermati un attimo!",
        "Valori decisamente 'strong'. Metti giù il telecomando, chiudi gli occhi e fai finta di essere un sasso."
    ],
    HYPERTENSIVE_CRISIS: [
        "Se fossi una pentola a pressione, faresti fischiare anche i vicini. Ti prego, rilassati subito!",
        "Valori da record, ma di quelli che non vogliamo premiare. Mettiti comodo e avvisa un medico.",
        "Il tuo cuore sta facendo un concerto heavy metal. È il momento di chiamare i soccorsi per sicurezza.",
        "Allarme rosso scuro. Non è uno scherzo: siediti, respira e contatta subito il dottore.",
        "I sensori stanno ballando il samba. Non ignorare questo numero: chiama subito il medico.",
        "Ok, spegni tutto. Il tuo sistema è in surriscaldamento globale. Contatta il dottore adesso.",
        "Valori da codice rosso fisso. Niente panico, ma siediti, respira e fatti dare un'occhiata da un professionista.",
        "Il cuore sta esagerando con gli effetti speciali. Mettiti comodo e chiama il medico per sicurezza."
    ],
    HYPOTENSION: [
        "Pressione così bassa che probabilmente stai fluttuando nello spazio. Un po' di sale?",
        "Sei così rilassato che il tuo cuore sembra stia facendo un pisolino. Sveglia!",
        "Valori da rettile in pieno inverno. Hai bisogno di un caffè o di una scossa di energia.",
        "Praticamente una mummia egizia. Forza, mangia qualcosa di salato e tirati su!",
        "Pressione così bassa che il tuo bracciale si sta chiedendo se sei ancora nella stanza."
    ],
    UNCLASSIFIED: [
        "I numeri non tornano. Hai provato a misurare la pressione al gatto?",
        "Dati confusi. O il bracciale è lento o hai inventato una nuova categoria medica.",
        "Errore di lettura. Cerca di stare fermo, non ridere e riprova il test.",
        "Niente da fare, lo sfigmomanometro non ti capisce. Riprova con più calma."
    ]
}

/**
 * Pick a random phrase for the given category.
 * Returns null ~30% of the time (no phrase shown).
 */
export function getRandomPhrase(category) {
    const pool = PHRASES[category]
    if (!pool || pool.length === 0) return null
    // ~30% chance of no phrase
    if (Math.random() < 0.3) return null
    return pool[Math.floor(Math.random() * pool.length)]
}
