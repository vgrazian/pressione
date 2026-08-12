/**
 * Ironic phrases shown after saving a reading.
 * Mapped by ReadingCategory key.
 *
 * Each phrase is either a plain string (gender-neutral) or an object
 * {m: "maschile", f: "femminile"} for gendered variants.
 * If gender is not set, the masculine form is used as default.
 *
 * Sometimes no phrase is shown (~30% chance).
 * A phrase already shown for the same category is never repeated consecutively.
 */

const REPETITION_KEY = 'iperTeso_lastPhrase'

const PHRASES = {
    NORMAL: [
        // --- Originali ---
        { m: "Pressione da manuale. Praticamente sei un monaco buddista in vacanza.", f: "Pressione da manuale. Praticamente sei una monaca buddista in vacanza." },
        "Tutto perfetto! Il tuo cuore batte al ritmo di una ballata rilassante.",
        "Valori così calmi che persino il tuo divano è geloso di tanta stabilità.",
        "Livello di zen: massimo. Puoi affrontare la riunione del lunedì senza paura.",
        { m: "Valori così perfetti che potresti fare il collaudatore di amache a livello professionale.", f: "Valori così perfetti che potresti fare la collaudatrice di amache a livello professionale." },
        // --- Nuove: Pressione ottimale ---
        "Questa non è pressione: è una recensione a cinque stelle del tuo sistema cardiovascolare.",
        "Il misuratore ha guardato il risultato e ha detto: «Va bene, non servo più».",
        "Pressione ottimale. Il cuore oggi è praticamente in smart working.",
        "Numeri così belli che quasi viene voglia di incorniciarli.",
        "Il tuo sangue oggi viaggia in prima classe, senza ritardi e senza coincidenze.",
        "Tutto perfetto. Persino il misuratore può andare in pausa caffè.",
        "Complimenti: oggi il tuo cuore non ha assolutamente voglia di fare straordinari.",
        // --- Nuove: Pressione normale ---
        "Perfetta: oggi il tuo cuore lavora con contratto regolare.",
        "La pressione è normale. Finalmente una cosa nella tua vita che non ha bisogno di essere discussa.",
        "Il misuratore ha detto sì. Il tuo cuore può tornare alle sue attività.",
        "Ottimo risultato: nessun drama cardiovascolare per oggi.",
        "La pressione è tranquilla. Praticamente l'unico membro della famiglia che non crea problemi.",
        "Tutto nella norma: puoi smettere di fissare quel numerino come se fosse l'esito di Sanremo.",
        "Il cuore oggi ha scelto la diplomazia."
    ],
    ELEVATED: [
        // --- Originali ---
        "Valori leggermente frizzanti. Qualcuno ha abusato del caffè stamattina?",
        "Un po' su di giri. Forse è colpa del traffico o di quel messaggio non letto.",
        "La pressione sale un briciolo. Fai tre respiri profondi e ignora i parenti.",
        "Un po' altina. È il momento ideale per sedersi e delegare i problemi altrui.",
        // --- Nuove: Pressione normale-alta ---
        "Sei nella zona «non facciamo drammi, ma diamoci un'occhiata».",
        "La pressione non è alta: è semplicemente in anticipo sull'ansia.",
        "Il cuore oggi ha detto: «Facciamo un pochino più forte, così ci divertiamo».",
        "Sei praticamente al confine: la pressione sta facendo turismo.",
        "Il misuratore ti ha dato un piccolo spoiler: rilassati un attimo.",
        "Non è una bocciatura, è un «potresti fare meglio» scritto in millimetri di mercurio.",
        "La pressione è nella categoria «bravo, però non montarti la testa»."
    ],
    HYPERTENSION_STAGE_1: [
        // --- Originali ---
        "Ok, il motore è caldo. Forse è il caso di scalare una marcia e rallentare.",
        "La tua pressione sta cercando di scalare l'Everest. Rimandala a valle.",
        { m: "Valori vivaci. Respira, conta fino a dieci e ricordati che non sei un supereroe.", f: "Valori vivaci. Respira, conta fino a dieci e ricordati che non sei una supereroina." },
        "Zona arancione! Metti giù quel sale e allontani la persona che ti sta stressando.",
        "Pressione in modalità 'pentola a pressione'. Comincia a far uscire un po' di vapore.",
        "Sei leggermente su di giri. Hai per caso incrociato lo sguardo del tuo capo?",
        "I numeri salgono. È il momento di attivare la modalità aereo nella tua testa.",
        "Valori frizzanti. Meno caffeina, meno drammi e più respiri profondi, grazie.",
        // --- Nuove: Ipertensione di grado 1 ---
        "La pressione è un po' alta. Evidentemente anche lei oggi ha delle aspettative.",
        "Il tuo cuore ha messo la modalità «lavoro straordinario» senza chiedere il permesso.",
        "Non preoccuparti: non sei agitato. È il tuo sangue che ha fretta.",
        "Il misuratore ti sta suggerendo, con molta diplomazia, di prenderti una pausa.",
        "La pressione è salita un pochino. Evidentemente ha ricevuto una buona notizia prima di noi.",
        "Direi che il tuo sistema cardiovascolare oggi è leggermente troppo motivato.",
        "Niente panico: al massimo il tuo sangue sta facendo un po' di networking."
    ],
    HYPERTENSION_STAGE_2: [
        // --- Originali ---
        "Calma! Il tuo cuore sta correndo la Formula 1 ma tu sei ancora sul divano.",
        "Decisamente alta. Fai finta di essere un bradipo per i prossimi venti minuti.",
        "Valori bollenti. È il segnale ufficiale per smettere di fare qualsiasi cosa.",
        "Il display è rosso. Diventa imperativo rilassarsi prima di trasformarsi in Hulk.",
        { m: "Il tuo cuore sta battendo i record di velocità di Hamilton. Peccato che tu sia seduto.", f: "Il tuo cuore sta battendo i record di velocità di Hamilton. Peccato che tu sia seduta." },
        "Sei a un passo dal trasformarti in un vulcano in eruzione. Trova un posto fresco e rilassati.",
        "La pressione è così alta che potresti gonfiare i pneumatici dell'auto. Fermati un attimo!",
        "Valori decisamente 'strong'. Metti giù il telecomando, chiudi gli occhi e fai finta di essere un sasso.",
        // --- Nuove: Ipertensione di grado 2 ---
        "La pressione non è alta: sta semplicemente cercando di vedere meglio il panorama.",
        "Complimenti, hai appena trasformato il braccio in una centrale idroelettrica.",
        "Questa non è pressione: è ambizione. Forse un po' troppa.",
        "Il misuratore ha visto il numero e ha chiesto: «Sicuro di voler continuare?»",
        "Direi che oggi il tuo sangue ha deciso di fare le scale di corsa.",
        "Tranquillo, non è un risultato: è un grido d'aiuto con i numeri.",
        "A questo punto non misuriamo più la pressione, chiediamo direttamente il meteo."
    ],
    HYPERTENSIVE_CRISIS: [
        "Se fossi una pentola a pressione, faresti fischiare anche i vicini. Ti prego, rilassati subito!",
        { m: "Valori da record, ma di quelli che non vogliamo premiare. Mettiti comodo e avvisa un medico.", f: "Valori da record, ma di quelli che non vogliamo premiare. Mettiti comoda e avvisa un medico." },
        "Il tuo cuore sta facendo un concerto heavy metal. È il momento di chiamare i soccorsi per sicurezza.",
        "Allarme rosso scuro. Non è uno scherzo: siediti, respira e contatta subito il dottore.",
        "I sensori stanno ballando il samba. Non ignorare questo numero: chiama subito il medico.",
        "Ok, spegni tutto. Il tuo sistema è in surriscaldamento globale. Contatta il dottore adesso.",
        "Valori da codice rosso fisso. Niente panico, ma siediti, respira e fatti dare un'occhiata da un professionista.",
        { m: "Il cuore sta esagerando con gli effetti speciali. Mettiti comodo e chiama il medico per sicurezza.", f: "Il cuore sta esagerando con gli effetti speciali. Mettiti comoda e chiama il medico per sicurezza." }
    ],
    HYPOTENSION: [
        // --- Originali ---
        "Pressione così bassa che probabilmente stai fluttuando nello spazio. Un po' di sale?",
        { m: "Sei così rilassato che il tuo cuore sembra stia facendo un pisolino. Sveglia!", f: "Sei così rilassata che il tuo cuore sembra stia facendo un pisolino. Sveglia!" },
        "Valori da rettile in pieno inverno. Hai bisogno di un caffè o di una scossa di energia.",
        "Praticamente una mummia egizia. Forza, mangia qualcosa di salato e tirati su!",
        "Pressione così bassa che il tuo bracciale si sta chiedendo se sei ancora nella stanza.",
        // --- Nuove: Pressione bassa ---
        "La pressione è così bassa che il sangue sta valutando se prendere il monopattino.",
        "Il tuo cuore oggi ha scelto la modalità risparmio energetico.",
        "Non sei stanco: sei semplicemente in modalità «batteria al 12%».",
        "La pressione è talmente tranquilla che sta praticamente facendo meditazione.",
        "Se ti alzi troppo velocemente, rischi di arrivare a destinazione prima della pressione.",
        "Il sangue oggi non corre: passeggia contemplando il paesaggio.",
        "Direi che il tuo sistema cardiovascolare ha preso il concetto di «prendersela con calma» un po' troppo sul serio."
    ],
    UNCLASSIFIED: [
        "I numeri non tornano. Hai provato a misurare la pressione al gatto?",
        "Dati confusi. O il bracciale è lento o hai inventato una nuova categoria medica.",
        { m: "Errore di lettura. Cerca di stare fermo, non ridere e riprova il test.", f: "Errore di lettura. Cerca di stare ferma, non ridere e riprova il test." },
        "Niente da fare, lo sfigmomanometro non ti capisce. Riprova con più calma."
    ]
}

/**
 * Resolve a phrase entry (string or {m,f} object) to a gendered string.
 * @param {string|{m:string, f:string}} entry
 * @param {'male'|'female'|null|undefined} gender
 * @returns {string}
 */
function resolveGender(entry, gender) {
    if (typeof entry === 'string') return entry
    return gender === 'female' ? entry.f : entry.m
}

/**
 * Pick a random phrase for the given category.
 * - Avoids repeating the last phrase shown for the same category.
 * - Returns null ~30% of the time (no phrase shown).
 * - Resolves gender variants if provided.
 *
 * @param {string} category - ReadingCategory key
 * @param {'male'|'female'|null|undefined} [gender] - User gender
 * @returns {string|null}
 */
export function getRandomPhrase(category, gender) {
    const pool = PHRASES[category]
    if (!pool || pool.length === 0) return null
    // ~30% chance of no phrase
    if (Math.random() < 0.3) return null

    // Read last phrase index for this category
    const storageKey = `${REPETITION_KEY}_${category}`
    const lastIndex = parseInt(localStorage.getItem(storageKey), 10)

    // Build list of eligible indices (exclude last if pool > 1)
    let eligible = pool.map((_, i) => i)
    if (eligible.length > 1 && !isNaN(lastIndex) && lastIndex >= 0 && lastIndex < pool.length) {
        eligible = eligible.filter(i => i !== lastIndex)
    }

    const idx = eligible[Math.floor(Math.random() * eligible.length)]
    localStorage.setItem(storageKey, String(idx))

    return resolveGender(pool[idx], gender)
}
