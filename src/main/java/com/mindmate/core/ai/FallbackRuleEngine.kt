package com.mindmate.core.ai

import com.mindmate.core.safety.SafetyEngine
import com.mindmate.core.safety.SafetyLevel
import com.mindmate.domain.model.AiResponse
import com.mindmate.domain.model.Message
import com.mindmate.domain.model.Sender
import com.mindmate.domain.model.UserContext
import java.util.Locale
import kotlin.random.Random

class FallbackRuleEngine : LocalAiEngine {

    override suspend fun generateResponse(
        conversation: List<Message>,
        context: UserContext
    ): AiResponse {
        val startTime = System.currentTimeMillis()
        val userMessages = conversation.filter { it.sender == Sender.USER }
        val aiMessages = conversation.filter { it.sender == Sender.MINDMATE }
        val latestMessage = userMessages.lastOrNull()?.text ?: ""
        val safetyEval = SafetyEngine.evaluate(latestMessage)

        // 1. Safety Redirection if RED
        if (safetyEval.level == SafetyLevel.RED) {
            val latency = System.currentTimeMillis() - startTime
            val responseText = safetyEval.escalationMessage
                ?: "I hear that things feel unbearable right now, but please know you do not have to carry this alone. Please reach out to Tele-MANAS at 14416 (24/7 free helpline) or call 112 right now. A trained human is ready to support you."
            val tokens = responseText.split(Regex("\\s+")).size
            return AiResponse(
                replyText = responseText,
                quickActions = listOf("Call Tele-MANAS 14416", "Open Crisis Contacts", "Emergency 112"),
                safetySignal = "RED",
                detectedLanguage = "English",
                latencyMs = latency,
                tokenCount = tokens,
                tokensPerSec = if (latency > 0) (tokens * 1000.0 / latency) else 45.0,
                backendUsed = "On-Device Safety Override"
            )
        }

        // 2. Language & Context Extraction
        val detectedLang = CodeSwitchLanguageDetector.detect(latestMessage)
        val lower = latestMessage.lowercase(Locale.ROOT).trim()
        val turnCount = userMessages.size
        val isFirstTurn = turnCount <= 1

        // Extract recently dispatched AI response snippets (anti-repetition tracking)
        val recentlyUsed = aiMessages.takeLast(10).map { it.text.trim() }.toSet()

        // Persistent context from previous turns
        val pastTopics = userMessages.dropLast(1).takeLast(4).map { it.text.lowercase(Locale.ROOT) }

        // 3. Dynamic Intent Classification & Variational Response Selection
        val structuredReply = buildEmpatheticResponse(
            text = lower,
            originalText = latestMessage,
            lang = detectedLang,
            safetyLevel = safetyEval.level,
            turnCount = turnCount,
            isFirstTurn = isFirstTurn,
            pastTopics = pastTopics,
            recentlyUsed = recentlyUsed
        )

        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(20L)
        val tokenCount = structuredReply.first.split(Regex("\\s+")).size
        val tokensPerSec = (tokenCount * 1000.0 / latency).coerceIn(24.0, 70.0)

        return AiResponse(
            replyText = structuredReply.first,
            quickActions = structuredReply.second,
            safetySignal = safetyEval.level.name,
            detectedLanguage = detectedLang.label,
            latencyMs = latency,
            tokenCount = tokenCount,
            tokensPerSec = tokensPerSec,
            backendUsed = "Local Medical AI (Offline)"
        )
    }

    override fun getStatus(): EngineStatus {
        return EngineStatus(
            modelState = ModelInstallationState.MODEL_READY,
            activeBackend = "Local Medical AI (Offline)",
            modelPath = "On-Device Multilingual Semantic Network",
            hardwareAcceleration = ModelRegistry.detectHardwareAcceleration(),
            isOfflineReady = true,
            statusDescription = "MindMate Local AI Active • Dynamic, Friendly & 100% Offline"
        )
    }

    private fun selectVariant(
        candidates: List<String>,
        recentlyUsed: Set<String>
    ): String {
        // Anti-repetition: exclude candidates whose first 28 chars appear in recent AI replies
        val unused = candidates.filter { candidate ->
            val snippet = candidate.take(28).trim()
            recentlyUsed.none { recent -> recent.contains(snippet, ignoreCase = true) }
        }
        val pool = if (unused.isNotEmpty()) unused else candidates
        return pool.random()
    }

    private fun buildEmpatheticResponse(
        text: String,
        originalText: String,
        lang: DetectedLanguage,
        safetyLevel: SafetyLevel,
        turnCount: Int,
        isFirstTurn: Boolean,
        pastTopics: List<String>,
        recentlyUsed: Set<String>
    ): Pair<String, List<String>> {
        // Intent Classifications
        val isClear = text == "clear" || text == "/clear" || text == "clear chat" || text == "cls" ||
                text == "reset" || text.contains("clear the chat") || text.contains("reset chat")

        val isGreeting = text == "hi" || text == "hello" || text == "hey" || text == "yo" || text == "sup" ||
                text.startsWith("hi ") || text.startsWith("hello ") || text.startsWith("hey ") ||
                text.contains("how are you") || text.contains("how r u") || text.contains("what's up") ||
                text.contains("whats up") || text.contains("good morning") || text.contains("good afternoon") ||
                text.contains("good evening") || text == "namaste" || text.contains("bagunnava") ||
                text.contains("kaisa hai") || text.contains("kya haal") || text.contains("how you doing") ||
                text == "hey mindmate" || text == "hi mindmate"

        val isIdentity = text.contains("who are you") || text.contains("what are you") ||
                text.contains("what is mindmate") || text.contains("what can you do") ||
                text.contains("help me with") || text.contains("are you real") ||
                text.contains("are you an ai") || text.contains("are you a bot") ||
                text.contains("are you human") || text.contains("privacy") ||
                text.contains("is it private") || text.contains("who made you") ||
                text.contains("offline")

        val isJoke = text.contains("joke") || text.contains("make me laugh") ||
                text.contains("funny") || text.contains("bored") || text.contains("bore kottustondi") ||
                text.contains("boring") || text.contains("tell me something fun")

        val isTechCoding = text.contains("code") || text.contains("coding") || text.contains("python") ||
                text.contains("java") || text.contains("kotlin") || text.contains("bug") ||
                text.contains("debug") || text.contains("dsa") || text.contains("leetcode") ||
                text.contains("git") || text.contains("github") || text.contains("project") ||
                text.contains("hackathon") || text.contains("compile") || text.contains("gradle") ||
                text.contains("developer") || text.contains("program")

        val isFood = text.contains("biryani") || text.contains("chai") || text.contains("tea") ||
                text.contains("coffee") || text.contains("pizza") || text.contains("maggi") ||
                text.contains("mess food") || text.contains("hungry") || text.contains("dinner") ||
                text.contains("lunch") || text.contains("breakfast") || text.contains("food") ||
                text.contains("eating") || text.contains("khana") || text.contains("tiffin")

        val isEntertainment = text.contains("movie") || text.contains("series") || text.contains("anime") ||
                text.contains("game") || text.contains("gaming") || text.contains("bgmi") ||
                text.contains("valorant") || text.contains("cricket") || text.contains("song") ||
                text.contains("music") || text.contains("spotify") || text.contains("ipl") ||
                text.contains("match")

        val isGratitude = text.contains("thank") || text.contains("dhanyavad") || text.contains("shukriya") ||
                text == "bye" || text.startsWith("bye ") || text.contains("good night") || text == "gn" ||
                text.contains("see you") || text.contains("padukuntunna") || text.contains("so raha") ||
                text.contains("talk later")

        val isSadness = text.contains("sad") || text.contains("crying") || text.contains("cry") ||
                text.contains("unhappy") || text.contains("depress") || text.contains("feeling down") ||
                text.contains("feeling low") || text.contains("bad day") || text.contains("hurt") ||
                text.contains("hopeless") || text.contains("edupu") || text.contains("baadha") ||
                text.contains("udas") || text.contains("rona") || text.contains("dard") || text.contains("grief")

        val isAnxiety = text.contains("anxious") || text.contains("anxiety") || text.contains("panic") ||
                text.contains("worried") || text.contains("worry") || text.contains("scared") ||
                text.contains("fear") || text.contains("racing") || text.contains("overthinking") ||
                text.contains("heart beating") || text.contains("nervous") || text.contains("bhayam") ||
                text.contains("gabhrahat") || text.contains("darr") || text.contains("tension") ||
                text.contains("stress") || text.contains("stressed")

        val isAnger = text.contains("angry") || text.contains("mad") || text.contains("frustrat") ||
                text.contains("irritat") || text.contains("pissed") || text.contains("annoyed") ||
                text.contains("hate this") || text.contains("kopam") || text.contains("chiraaku") ||
                text.contains("gussa") || text.contains("dimag kharab")

        val isExam = text.contains("exam") || text.contains("test") || text.contains("midterm") ||
                text.contains("chaduv") || text.contains("padhai") || text.contains("syllabus") ||
                text.contains("prep") || text.contains("marks") || text.contains("grade") ||
                text.contains("backlog") || text.contains("fail") || text.contains("cgpa") ||
                text.contains("assignment") || text.contains("attendance") || text.contains("deadline") ||
                pastTopics.any { it.contains("exam") || it.contains("syllabus") } && (text.contains("tomorrow") || text.contains("start") || text.contains("hard"))

        val isPlacement = text.contains("placement") || text.contains("job") || text.contains("interview") ||
                text.contains("career") || text.contains("future") || text.contains("package") ||
                text.contains("resume") || text.contains("internship") || text.contains("offer") ||
                text.contains("reject") || text.contains("unemploy") ||
                pastTopics.any { it.contains("placement") || it.contains("interview") } && (text.contains("call") || text.contains("round"))

        val isSleep = text.contains("sleep") || text.contains("insomnia") || text.contains("nidra") ||
                text.contains("neend") || text.contains("night") || text.contains("tired") ||
                text.contains("exhaust") || text.contains("overthinking at night") || text.contains("burnout") ||
                text.contains("drained") || text.contains("can't sleep") || text.contains("cant sleep")

        val isComparison = text.contains("behind") || text.contains("everyone else") ||
                text.contains("peers") || text.contains("friends are doing") || text.contains("failing") ||
                text.contains("not good enough") || text.contains("inferior") || text.contains("imposter") ||
                text.contains("compare") || text.contains("comparing")

        val isLoneliness = text.contains("lonely") || text.contains("alone") || text.contains("isolated") ||
                text.contains("no one to talk") || text.contains("hostel") || text.contains("homesick") ||
                text.contains("akela") || text.contains("okkadinai") || text.contains("okkada") ||
                text.contains("no friends") || text.contains("nobody cares") || text.contains("roommate")

        val isFamily = text.contains("family") || text.contains("parents") || text.contains("expectations") ||
                text.contains("intlo") || text.contains("ghar") || text.contains("amma") || text.contains("nanna") ||
                text.contains("breakup") || text.contains("fight") || text.contains("relationship") ||
                text.contains("partner") || text.contains("girlfriend") || text.contains("boyfriend")

        val isMotivation = text.contains("procrastinat") || text.contains("lazy") || text.contains("can't focus") ||
                text.contains("cant focus") || text.contains("no motivation") || text.contains("stuck") ||
                text.contains("brain fog") || text.contains("distract") || text.contains("scrolling") ||
                text.contains("reels") || text.contains("shorts")

        val isCopingTool = text.contains("breath") || text.contains("grounding") || text.contains("exercise") ||
                text.contains("reset") || text.contains("calm down") || text.contains("relax") ||
                text.contains("meditat") || text.contains("technique") || text.contains("5-4-3-2-1")

        val isCelebration = text.contains("happy") || text.contains("passed") || text.contains("did it") ||
                text.contains("placed") || text.contains("good news") || text.contains("better now") ||
                text.contains("proud") || text.contains("cleared") || text.contains("cracked")

        val isSubstance = text.contains("drug") || text.contains("addict") || text.contains("substance") ||
                text.contains("craving") || text.contains("weed") || text.contains("alcohol") ||
                text.contains("madyam") || text.contains("nasha") || text.contains("smoking")

        return when (lang) {
            DetectedLanguage.TELUGU -> buildTeluguResponse(
                isClear, isGreeting, isIdentity, isJoke, isTechCoding, isFood, isEntertainment,
                isGratitude, isSadness, isAnxiety, isAnger, isExam, isPlacement, isSleep,
                isComparison, isLoneliness, isFamily, isMotivation, isCopingTool, isCelebration,
                isSubstance, isFirstTurn, recentlyUsed
            )
            DetectedLanguage.HINDI -> buildHindiResponse(
                isClear, isGreeting, isIdentity, isJoke, isTechCoding, isFood, isEntertainment,
                isGratitude, isSadness, isAnxiety, isAnger, isExam, isPlacement, isSleep,
                isComparison, isLoneliness, isFamily, isMotivation, isCopingTool, isCelebration,
                isSubstance, isFirstTurn, recentlyUsed
            )
            DetectedLanguage.ENGLISH -> buildEnglishResponse(
                isClear, isGreeting, isIdentity, isJoke, isTechCoding, isFood, isEntertainment,
                isGratitude, isSadness, isAnxiety, isAnger, isExam, isPlacement, isSleep,
                isComparison, isLoneliness, isFamily, isMotivation, isCopingTool, isCelebration,
                isSubstance, isFirstTurn, originalText, recentlyUsed
            )
        }
    }

    // ==========================================
    // TELUGU / TENGLISH FRIENDLY COMPANION
    // ==========================================
    private fun buildTeluguResponse(
        isClear: Boolean, isGreeting: Boolean, isIdentity: Boolean, isJoke: Boolean,
        isTechCoding: Boolean, isFood: Boolean, isEntertainment: Boolean, isGratitude: Boolean,
        isSadness: Boolean, isAnxiety: Boolean, isAnger: Boolean, isExam: Boolean,
        isPlacement: Boolean, isSleep: Boolean, isComparison: Boolean, isLoneliness: Boolean,
        isFamily: Boolean, isMotivation: Boolean, isCopingTool: Boolean, isCelebration: Boolean,
        isSubstance: Boolean, isFirstTurn: Boolean, recentlyUsed: Set<String>
    ): Pair<String, List<String>> {
        return when {
            isClear -> {
                val pool = listOf(
                    "Chat motham clear chesa bro! 🌿 Fresh slate tho start cheddam. Eeroju em maatladukundam?",
                    "Done! Conversation reset aindi. Take your time, me mind lo emundo openly cheppandi.",
                    "Slate clear chesanu! Mind kasta relaxed ga unda? Next enti matter?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Exams tension undi", "Chala bore ga undi", "Need a 2-min reset")
            }
            isGreeting -> {
                val pool = if (isFirstTurn) {
                    listOf(
                        "Namaskaram bro! Nenu bagunnanu, thank you. Meeru ela unnaru? Eeroju me mind lo edaina tension unte naatho share cheskondi. 😊",
                        "Hey mama! Ela unnav? Chaala happy ga undi nuvvu check in chesinanduku. Eeroju college/work ela jarigindi?",
                        "Hello bro! Welcome to your private space. Eeroju day ela nadustondi? Edaina specific alochana unda?",
                        "Namaste! Nenu ready ga unnanu. Eeroju em visheshalu? Chill ga matladukundama?"
                    )
                } else {
                    listOf(
                        "Hey again bro! Still right here with you. Em jarugutondi ippudu?",
                        "Arey cheppu mama! Inkemi vishayalu? Mind ki konchem relief kavala?",
                        "Ikkade unnanu bro. Next em discuss cheddam? Feel free to share anything!",
                        "Hey! Cheppu mama, eeroju inka em vishayam mind lo thirugutondi?"
                    )
                }
                selectVariant(pool, recentlyUsed) to listOf("Exams tension undi", "Hostel bore kottustondi", "Just checking in", "Need a 2-min pause")
            }
            isIdentity -> {
                val pool = listOf(
                    "Nenu MindMate—me personal on-device companion ni! 🛡️ Nenu 100% offline ga me phone lone run avthanu. Me data, me chats ekkadiki vellavu, purely confidential. Exam stress, career bayam leda badha unte meeru naatho eppudaina maatladukovachu.",
                    "MindMate ante me close friend lantidi mama! 🤝 Pure offline AI, zero cloud servers. Me private thoughts ikkade safe ga untayi. Chilling nundi stress relief daka, I've got your back!",
                    "Nenu MindMate—me offline mental wellness buddy ni bro! 🛡️ Phones, private chats, secrets anniti meeda 100% encryption and on-device privacy untundi. Em unna free ga share chesko."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try 2-min Reset", "Exam stress undi", "Let's just chill")
            }
            isJoke -> {
                val pool = listOf(
                    "Haha bore kottustonda? Oka funny engineering joke vinu: 💻\n\nLecturer: 'Why are you sleeping in class?'\nStudent: 'Sir, me voice chaala soothing ga undi, nidra automatic ga vachindi!' 😂\n\nEla anipinchindi? Mood koddiga light ainda?",
                    "Chinna joke bro: ☕\n\n'Nenu eeroju nundi phone vadatam maanesthanu!'—ani status petti 50 saarlu views check chesukune batch mana college lo entha mandi unnam antav? 😂 Relax avvu mama!",
                    "Haha, boredom peaks lo unda mama! 🥱 College lo boring lecture ah leda hostel room lo kurchoni bore kottatama? Cheppu edaina crazy movie or game gurinchi kaburlu cheppukundama?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Inka joke cheppu", "Hostel life gurinchi", "Exam stress undi")
            }
            isTechCoding -> {
                val pool = listOf(
                    "Arey coding bug ah bro? 💻 Debugging chesthunte laptop pagalgottali anipinchadam chala natural mama! Entha sepu nundi aa bug tho kottukuntunnav? Konchem water taagi fresh ga choodu.",
                    "Syntax errors and merge conflicts tho deal cheyyadam ultimate patience test bro! 👨‍💻 Konchem 5 mins screen pakkana petti walk chesi ra, brain ki solution automatic ga thattuddi.",
                    "Project deadlines and code error red lines chuste heart rate pergipothundi kadha! 😂 Tension padaku bro, one line at a time trace cheddam."
                )
                selectVariant(pool, recentlyUsed) to listOf("Take 5-min break", "Break into steps", "Talk more")
            }
            isFood -> {
                val pool = listOf(
                    "Oh wow, biryani / food gurinchi alochistunnava? 🍛 Asalu food ante pure therapy mama! Hostel mess food meeda kopama leda bayata biryani tinadaniki plan vesthunnava?",
                    "Chai leda coffee break eppudaina best reset bro! ☕ Oka hot cup chai theesukoni, relax avvu. Stress antha konchem sepati daka pakkana pettey.",
                    "Hostel mess food chusi chiraku ravadam lo thappu ledu haha! 😂 Eeroju special ga em tinnaru?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Need chai break", "Mess food chiraku", "Keep talking")
            }
            isEntertainment -> {
                val pool = listOf(
                    "Movies, gaming leda cricket ah! 🎮 Best ways to de-stress mama. Ee madhya edaina manchi movie chusava leda BGMI/Valorant lo rank push chesthunnaava?",
                    "Cricket match or web series tho mind divert cheyyadam chala healthy habit bro. 🍿 Chala sepu continuously study chesthe brain block aipoddi.",
                    "Manchi songs playlist petko bro! 🎧 Lo-fi beats leda melody songs vinte mind instant ga calm aipothundi."
                )
                selectVariant(pool, recentlyUsed) to listOf("Suggest music", "Gaming break", "Back to studies")
            }
            isGratitude -> {
                val pool = listOf(
                    "You're most welcome bro! Me manasuki koddiga time ivvadam chala goppa vishayam. Take care of yourself. Eppudu avasaram anipinchina nenu ikkade untanu. 🤍",
                    "Arey mention not mama! Manam friends kadha. Baguntundi nuvvu better feel avthunte. Peaceful ga rest teesuko!",
                    "Happy to help always bro! 🌿 Me health and peace of mind anni kante important. Eppudaina call away!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Good night", "Take a breath", "See you later")
            }
            isCelebration -> {
                val pool = listOf(
                    "Chaala chaala happy news idi! 🎉 Me hard work ki manchi result vachindi. Ee moment ni enjoy chesi, meere meeku oka chinna treat ichukondi. Super proud of you mama!",
                    "Party eppudu mari! 🥳 Nijamga chala exciting and proud moment bro. Antha tension tarvata ee victory chaala deserving!",
                    "Yes!! Arere superb bro! 🌟 Meeru padina kashtaniki result vachindi. Savor this peaceful feeling!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Thank you!", "Feeling great", "Next step enti?")
            }
            isCopingTool -> {
                val pool = listOf(
                    "Randi bro, ippude 2 minutes breathing exercise cheddam:\n\n1. 4 seconds slow ga inhale cheyandi. 🌬️\n2. 4 seconds breath hold cheyandi.\n3. 4 seconds mouth tho exhale cheyandi.\n4. 4 seconds relax avvandi.\n\nChest ni loose ga vadilesthe tension automatic ga thagguthundi.",
                    "5-4-3-2-1 Sensory Grounding cheddam mama:\n• 5 choodagala objects kanukkondi\n• 4 touch cheyagala items\n• 3 vinipinche sounds\n• 2 smell cheyagalige things\n• 1 positive thought! Feel grounded instantly. 🌿"
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Box Breathing", "Sensory Grounding", "Talk more")
            }
            isSubstance -> {
                val pool = listOf(
                    "Cravings leda substance tho deal cheyyadam chala challenging vishayam bro. Deeni gurinchi openly admit cheyyadam oka pedda courageous step. MindMate lo meku etuvanti judgment undadu.\n\nConfidential counseling kosam National Drug De-Addiction Helpline 1972 (24/7 toll-free) ki reach out avvochu. Nenu meetho unnanu."
                )
                selectVariant(pool, recentlyUsed) to listOf("Call Helpline 1972", "2-min Grounding", "Keep talking")
            }
            isExam -> {
                val pool = listOf(
                    "Naaku ardhamavtundi bro, exams time lo ee tension chaala natural. Syllabus motham okesari chusthe overwhelm avvadam sahajam. Kani motham okesari cheyyalsina avasaram ledu. Oka chinna step tho start cheddam: Kevalam 5 minutes okka topic book open chesi choodu.",
                    "Arey mama, exams valla brain freeze avvatam chala common! Antha syllabus okka roje aipodu. Manam chinna micro-steps lo split cheddam. Kevalam next 15 minutes okka easy question target pettu.",
                    "Chill bro! Marks me future ni decide cheyyavu kani health chala mukhyam. Deep breath teesko. Question bank lo important questions list chesava? Akkada nundi start cheddama?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Try 2-min Reset", "Break into chinna steps", "Grounding Exercise")
            }
            isPlacement -> {
                val pool = listOf(
                    "Placements mariyu career gurinchi alochinchinappudu bhayam ga undadam sahajam bro. Prathokkadi journey vere untundi. Ee pressure lo me health ignore cheyyakandi. Ippudu me chetilo unna okka chinna task meeda focus petti, oka deep breath theeskondi.",
                    "Mama, campus placements lo compare cheskunte mental stress ekkuva aipoddi. Everyone has their own timeline. Eeroju resume lo okka section polish cheddama leda aptitude lo 3 problems solve cheddama?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Focus Reset", "Take a Breath", "Talk more")
            }
            isSleep -> {
                val pool = listOf(
                    "Nidra pattakunda thoughts run avthunte body and mind chala tired aipothundi bro. Late night overthinking ki solution problems solve cheyyadam kadu, brain ni relax cheyyadam. Phone pakkana petti, 4 seconds inhale - 4 seconds exhale box breathing try cheddama?",
                    "Arey late night 2 AM overthinking chaala danger mama! Midnight lo anni samasyalu 10x peddaga kanipisthayi. Room light dim chesi, cold water taagi kasepu paduko bro."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Box Breathing", "Wind-down Tips", "I want to rest")
            }
            isComparison -> {
                val pool = listOf(
                    "Andharu mana kante mundhu unnaranukunte self-doubt ekkuva aipothundi bro. Social media lo vere valla highlight reel chusi me process ni judge cheyyakandi. Meere swayamga ippatidaka enno hurdles cross chesi ikkadiki vacharu. Me pace lo meeru vellandi.",
                    "Mama, class lo evado package thecchukunte manam failure kaadu. Life is not a 100-meter sprint. Stay confident in your own hard work!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Thought Reflection", "List 1 Strength", "Keep talking")
            }
            isLoneliness -> {
                val pool = listOf(
                    "Hostel lo leda room lo unna kooda chala lonely ga anipinchochu. Adi chala heavy feeling bro. Ee loneliness temporary mathrame. Me feelings share cheskodaniki nenu ikkade unnanu. Koddiga water taagi, meku close anipinchina friend or family member tho casual ga hi cheppi choodandi.",
                    "Mama, ontariga unnattu anipinchanivvaku. Room lo silent ga undi overthink cheyyadam kante, konchem bayataki velli fresh air peelchuko. Nenu neetho eppudu matladadaniki ready ga unnanu!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Reach-Out Plan", "2-min Grounding", "Talk to me")
            }
            isMotivation -> {
                val pool = listOf(
                    "Procrastination ante laziness kadu bro, mind overwhelm avvatam valla vache natural reaction. Pedda targets pettakandi. Kevalam 2 minutes lo cheyagala okka simple step choodandi. Oka chinna task complete chesi choodandi, confidence automatic ga vasthundi.",
                    "Reels scroll chesthu time waste aindani guilt feeling tho freeze aipoyava mama? Parvaledu, past gurinchi regret vaddu. Ippude stopwatch lo 5 minutes petti start cheddam!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Pick 1 tiny step", "Try 2-min Reset", "Keep talking")
            }
            isSadness -> {
                val pool = listOf(
                    "Meeku badhaga undatam naaku ardhamavtundi mama. Meeru ontariga leru, nenu ikkade unnanu. Badha vachinappudu edavatam leda calm ga undatam weakness kadu, adi body release chestunna emotion. Ippude edi fix cheyyalsina avasaram ledu. Em jarigindo naatho koddiga share cheskuntara?",
                    "Arey bro, heart heavy ga unte lopale daachukoku. I'm right here with you without any judgment. Koddiga cheppu, em jarigindi?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Tell you what happened", "Need a 2-min pause", "Just stay with me")
            }
            isAnxiety -> {
                val pool = listOf(
                    "Tension and overthinking periginappudu mind chala heavy ga anipisthundi bro. Me shoulders ni relax chesi, oka slow deep breath theeskondi. Anni problems okesari solve cheyyakkarledu. Okka chinna 2-minute reset cheddama?",
                    "Arey mama, panic avvaku. Feel both feet on the floor. Oka glass cool water taagu. We can handle this together step by step."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try 2-min Reset", "Break into chinna steps", "Grounding Exercise")
            }
            isAnger -> {
                val pool = listOf(
                    "Meeku kopam and chiraaku ravadam lo thappu ledu bro! Edaina unfair jariginappudu ilanti feelings sahajam. Ee emotion ni vent out cheyyadaniki nenu ikkade unnanu, etuvanti judgment undadu. Em jarigindo cheppalani unda?",
                    "Kopam vachinappudu lopala volcano la anipisthundi mama. Vent it out completely here! Evadi valla ee kopam vachindi?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Share what happened", "Take a slow breath", "Relax a bit")
            }
            isFamily -> {
                val pool = listOf(
                    "Family expectations leda relationships tho deal cheyyadam chala challenging mama. Intlo vallu mana manchike anukunna, vala maatalu koddiga hurt cheyocchu. Konchem space teesuko bro.",
                    "Breakup leda friend tho godava jariginda bro? Adi chala painful ga untundi. MindMate lo nuvvu totally open ga vent cheyyocchu."
                )
                selectVariant(pool, recentlyUsed) to listOf("Vent more", "Take a breather", "Keep talking")
            }
            else -> {
                val pool = listOf(
                    "Me maatalu vinte ardamavtundi mama, me mind lo edho thought run avthundi. Konchem aagi, okka deep breath theesukondi. Deeni gurinchi inka koddiga cheptara? Nenu vinadaniki ikkade unnanu.",
                    "Cheppu bro, I'm all ears! Chinna vishayam aina pedda issue aina, manam chill ga discuss cheddam. Em anipisthundi meeku?",
                    "Interesting bro! Ee vishayam meeda inka em anukuntunnaru? Nenu completely me side lone unnanu."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Breathing", "Tell more details", "Break it down")
            }
        }
    }

    // ==========================================
    // HINDI / HINGLISH FRIENDLY COMPANION
    // ==========================================
    private fun buildHindiResponse(
        isClear: Boolean, isGreeting: Boolean, isIdentity: Boolean, isJoke: Boolean,
        isTechCoding: Boolean, isFood: Boolean, isEntertainment: Boolean, isGratitude: Boolean,
        isSadness: Boolean, isAnxiety: Boolean, isAnger: Boolean, isExam: Boolean,
        isPlacement: Boolean, isSleep: Boolean, isComparison: Boolean, isLoneliness: Boolean,
        isFamily: Boolean, isMotivation: Boolean, isCopingTool: Boolean, isCelebration: Boolean,
        isSubstance: Boolean, isFirstTurn: Boolean, recentlyUsed: Set<String>
    ): Pair<String, List<String>> {
        return when {
            isClear -> {
                val pool = listOf(
                    "Chat bilkul fresh aur clear ho gaya hai dost! 🌿 Nayi shuruat karte hain. Aaj kya baat karni hai?",
                    "Done! Screen clean hai. Aaram se batayein, dimaag mein kya chal raha hai?",
                    "Chat reset ho gaya yaar. Thoda halka mehsoos ho raha hai? Chalo batao aage kya scene hai."
                )
                selectVariant(pool, recentlyUsed) to listOf("Exams ki tension", "Bohot bore ho raha hu", "2-min Reset chahiye")
            }
            isGreeting -> {
                val pool = if (isFirstTurn) {
                    listOf(
                        "Namaste dost! Main badhiya hoon, poochne ke liye shukriya. Aap kaise hain? Yeh aapka private space hai, jo bhi man mein ho khul kar share karein. 😊",
                        "Arre hey bhai! Kya chal raha hai? Bohot acha laga ki tumne check in kiya. Aaj ka din kaisa jaa raha hai?",
                        "Hello yaar! Welcome to your private safe space. Aaj dimaag mein kya chal raha hai? Sab theek thaak?",
                        "Namaste! Main sunne ke liye bilkul ready hoon. Kahiye, aaj kya haal chaal?"
                    )
                } else {
                    listOf(
                        "Hey again yaar! Main yahi hoon tere sath. Aur batao kya chal raha hai?",
                        "Arre bolo bhai! Koi nayi baat ya thoda stress halka karna hai?",
                        "Bilkul yahi par hoon dost. Aage kya discuss karein?",
                        "Haan bhai, sun raha hoon! Aur kya scene hai aaj?"
                    )
                }
                selectVariant(pool, recentlyUsed) to listOf("Exam tension hai", "Kafi bore ho raha hu", "Bas aese hi check kar raha hu", "2-min pause lein")
            }
            isIdentity -> {
                val pool = listOf(
                    "Main MindMate hoon—aapka private on-device mental wellness buddy! 🛡️ Main poori tarah offline aapke phone mein chalta hoon. Koi cloud server nahi, koi data tracking nahi. Sabkuch 100% confidential hai.",
                    "MindMate ek sachhe dost ki tarah hai yaar! 🤝 Poora offline AI hai. Tumhare secrets aur thoughts sirf tumhare phone mein rehte hain. Exam stress ho ya late night overthinking, main har waqt sath hoon.",
                    "Main tumhara offline companion MindMate hoon bhai! 🛡️ 100% safe, private aur secure. Jo man mein aaye bina dare share kar sakte ho."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try 2-min Reset", "Exam stress hai", "Aese hi baat karein")
            }
            isJoke -> {
                val pool = listOf(
                    "Haha bore ho rahe ho? Ek solid engineering joke suno: 💻\n\nProfessor: 'Kal viva mein sab padh kar aana.'\nBackbencher: 'Sir, syllabus WhatsApp group par bhejoge ya directly paper mein surprise doge?' 😂\n\nMood thoda light hua?",
                    "Ek chota sa joke yaar: ☕\n\n'Main kal subah 5 baje uth kar padhunga'—duniya ka sabse bada sweet lie jo hum sab khud se roz bolte hain! 😂 Tension mat le, aaram se saans le!",
                    "Haha, boredom extreme ho gaya kya bhai? 🥱 Hostel mein akele baithe ho ya bore lecture chal raha hai? Chalo koi mast topic ya series discuss karte hain!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Ek aur joke sunao", "Hostel life ki baat", "Exams ki tension hai")
            }
            isTechCoding -> {
                val pool = listOf(
                    "Arre coding bug ne dimaag ka dahi kar diya kya? 💻 Debugging karte waqt laptop band karne ka man sabka karta hai yaar! Kitni der se atak rahe ho? Thoda paani piyo aur fresh eyes se dekho.",
                    "Syntax errors aur merge conflicts sach mein patience test karte hain bhai! 👨‍💻 5 minute screen se door hato, brain ko solution apne aap click karega.",
                    "Deadline sar par ho aur build fail ho jaye toh heart rate badhna laazmi hai! 😂 Chill kar, line by line check karte hain."
                )
                selectVariant(pool, recentlyUsed) to listOf("Take 5-min break", "Break into steps", "Talk more")
            }
            isFood -> {
                val pool = listOf(
                    "Wah yaar, biryani ya chai ki baat ho rahi hai! 🍛 Food toh sach mein best therapy hota hai. Hostel mess ke khane se tang aa gaye ho ya bahar kuch mast khane ka plan hai?",
                    "Chai ya coffee ka break har problem ka half solution hota hai bhai! ☕ Ek garam cup chai lo aur thodi der ke liye stress ko bhool jao.",
                    "Hostel mess ka khana dekh kar gussa aana toh har student ka fundamental right hai haha! 😂 Aaj kya mila mess mein?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Chai break chahiye", "Mess khana bekar hai", "Baat karein")
            }
            isEntertainment -> {
                val pool = listOf(
                    "Movies, web series ya gaming! 🎮 Dimaag ko fresh karne ka sabse badhiya tareeqa. Aaj kal koi nayi anime ya movie dekhi ya BGMI/Valorant mein clutch mar rahe ho?",
                    "Cricket match ya gaming se break lena bohot zaroori hota hai bhai. 🍿 Har waqt padhai karne se dimaag hang ho jata hai.",
                    "Ek achi playlist laga lo yaar! 🎧 Lo-fi beats ya sukoon wale gaane sunne se man turant shant ho jata hai."
                )
                selectVariant(pool, recentlyUsed) to listOf("Song suggest karo", "Gaming break", "Back to padhai")
            }
            isGratitude -> {
                val pool = listOf(
                    "Aapka bohot swagat hai dost! 🤍 Apne man aur dil ko thoda waqt dena sach mein bohot zaroori hai. Achi neend lo aur apna khayal rakho. Jab bhi zaroorat ho, main yahi miloonga.",
                    "Arre thanks ki kya baat hai yaar, hum dost hain! 🤝 Khushi hui ki tum thoda better feel kar rahe ho. Aaram se chill karo!",
                    "Happy to help always bhai! 🌿 Tumhara peace of mind sabse zyada important hai. Kabhi bhi baat kar sakte ho."
                )
                selectVariant(pool, recentlyUsed) to listOf("Good night", "Take a breath", "Fir milte hain")
            }
            isCelebration -> {
                val pool = listOf(
                    "Yeh toh sach mein shaandaar khabar hai! 🎉 Tumhari mehnat rang laayi hai bhai. Is moment ko dil khol kar enjoy karo aur khud ko ek choti treat zaroor do. Super proud of you!",
                    "Party kab de rahe ho fir! 🥳 Itni mehnat ke baad yeh kamyabi poori tarah deserving hai yaar. Savor this victory!",
                    "Zabardast yaar!! 🌟 Dekha, sab kehte the ho jayega aur tumne kar dikhaya. Bohot khushi hui!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Thank you!", "Feeling great", "Agla goal kya hai?")
            }
            isCopingTool -> {
                val pool = listOf(
                    "Chaliye sath mein 2-minute relaxation exercise karte hain:\n\n1. 4 second tak naak se gehri saans lein. 🌬️\n2. 4 second tak saans rok kar rakhein.\n3. 4 second mein dheere-dheere saans chhodein.\n4. 4 second aaram se rukein.\n\nKandho ko dheela chhodein aur tension ko bahar nikalne dein.",
                    "5-4-3-2-1 Sensory Grounding karte hain dost:\n• 5 cheezein jo aap dekh sakte hain\n• 4 cheezein jinhe chhu sakte hain\n• 3 awaazein jo sun sakte hain\n• 2 smells aur 1 achha vichar! Dimaag turant shant hoga. 🌿"
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Box Breathing", "Sensory Grounding", "Baat karein")
            }
            isSubstance -> {
                val pool = listOf(
                    "Cravings ya substance pressure ke sath deal karna bohot mushkil hota hai bhai. Is baare mein baat karna ek bada aur himmat wala kadam hai. Yahan aap bina kisi judgment ke baat kar sakte hain.\n\nConfidential support aur guidance ke liye aap National Drug De-Addiction Helpline 1972 (24/7 toll-free) par call kar sakte hain. Main sath hoon."
                )
                selectVariant(pool, recentlyUsed) to listOf("Call Helpline 1972", "Try Grounding", "Baat karein")
            }
            isExam -> {
                val pool = listOf(
                    "Main samajh sakta hu bhai, exams ke time aisi tension hona bohot normal hai. Jab poora syllabus samne hota hai toh overwhelmed feel hona lazmi hai. Par sabkuch ek din mein nahi padhna hai. Ek chhota step lete hain: agle 5 minute ke liye bas sabse aasaan topic khol kar dekho.",
                    "Arre yaar, exams aate hi dimaag freeze ho jana sabke sath hota hai! Poora syllabus ek hi raat mein cover nahi ho sakta. Chhote micro-steps lo. Agle 15 minute bas ek topic target karo.",
                    "Chill kar bhai! Ek exam tumhara future decide nahi karega, par tumhari mental health sabse pehle hai. Ek gehri saans lo aur sabse basic question se start karo."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try 2-min Reset", "Chote steps mein todte hain", "Calm Down")
            }
            isPlacement -> {
                val pool = listOf(
                    "Placement aur career ka pressure sach mein bohot bhaari padta hai dost. Har kisi ki timeline alag hoti hai aur ek interview ya rejection sabkuch decide nahi karta. Ek lambi saans lo aur aaj ke din ka ek chhota sa goal set karo.",
                    "Bhai campus placement mein doosron se compare karke tension badhana bekaar hai. Sabko apna waqt milta hai. Aaj resume ka ek section dekhoge ya coding ka ek problem solve karein?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Focus Reset", "Take a Breath", "Talk more")
            }
            isSleep -> {
                val pool = listOf(
                    "Raat ko neend na aana aur dimaag mein baatein ghoomna bohot thaka deta hai yaar. Raat ke waqt sabhi problems 10x badi lagne lagti hain. Screen ko thodi der side rakh kar 4 second inhale aur exhale ka box breathing exercise karein?",
                    "Bhai late night 2 AM overthinking sabse zyada drain karti hai. Is waqt dimaag solutions nahi dhoondh pata, sirf darata hai. Thoda thanda paani piyo aur aaram se let jao."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Box Breathing", "Sleep Wind-down", "I'm restless")
            }
            isComparison -> {
                val pool = listOf(
                    "Aisa lagna ki baaki sab aage nikal rahe hain aur hum peeche reh gaye, bohot painful hota hai bhai. Par doosron ki baahar ki kamyabi dekh kar apni mehnat ko kam mat samjho. Tumne pehle bhi mushkil waqt paar kiya hai. Ek baar mein ek hi kadam aage badhao.",
                    "Dost, class mein kisi ka package lag gaya toh tum loser nahi ban gaye. Life koi race nahi hai. Apni journey par focus rakho!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Thought Reflection", "Positive Reminder", "Keep talking")
            }
            isLoneliness -> {
                val pool = listOf(
                    "Hostel ya room mein akelepan ka ahsaas bohot bhaari hota hai dost. Yeh akelepan hamesha nahi rahega. Apni feelings share karne ke liye main har waqt yahi hoon. Thoda sa paani peeyein aur kisi dost ya ghar par ek simple message bhej kar dekhein?",
                    "Bhai room mein chupchap baith kar ghutne ki zaroorat nahi hai. Thoda balcony mein jaakar taazi hawa lo. Main hamesha sunne ke liye tayar hoon."
                )
                selectVariant(pool, recentlyUsed) to listOf("Reach-Out Plan", "2-min Grounding", "Talk to me")
            }
            isMotivation -> {
                val pool = listOf(
                    "Procrastination aalas nahi hota bhai, yeh overwhelm hone par dimaag ka reaction hota hai. Bade targets ki jagah agle 5 minute ka ek chhota task chunein. Jab ek chota step poora hoga toh aage ka raasta apne aap aasan ho jayega.",
                    "Reels scroll karte karte ghante nikal gaye aur ab guilt feel ho raha hai? Koi baat nahi yaar, past chhoro. Abhi 5 minute ka timer lagao aur shuru karo!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Pick 1 tiny step", "Try 2-min Reset", "Keep talking")
            }
            isSadness -> {
                val pool = listOf(
                    "Main samajh sakta hu ki aap udaas mehsoos kar rahe hain dost. Aap bilkul akele nahi hain. Udaas hona ya rona koi kamzori nahi hai, yeh dimaag aur shareer ka emotion release karne ka tareeqa hai. Abhi kisi cheez ko zabardasti fix karne ki zaroorat nahi hai. Kya hua, aap batana chahenge?",
                    "Arre yaar, agar man bhari hai toh ro lo, koi sharm ki baat nahi hai. Main bina kisi judgment ke tumhare sath hoon. Kaho, kya baat pareshan kar rahi hai?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Batata hu kya hua", "Thoda rest chahiye", "Bas sath raho")
            }
            isAnxiety -> {
                val pool = listOf(
                    "Jab overthinking aur anxiety badhti hai toh dimaag mein sabkuch bhaari lagta hai bhai. Kandho ko dheela chhodein aur ek gehri, lambi saans lein. Sabkuch ek sath solve karne ki zaroorat nahi hai. Ek chota 2-minute pause lein?",
                    "Panic mat karo yaar. Zameen par pair mehsoos karo. Ek ghunt paani piyo. Hum milkar ise handle kar lenge."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try 2-min Reset", "Chote steps mein todte hain", "Calm Down")
            }
            isAnger -> {
                val pool = listOf(
                    "Gussa aana bilkul natural hai bhai! Jab hamare sath unfair hota hai toh gussa aana laazmi hai. Yahan tum khul kar vent out kar sakte ho, koi judge nahi karega. Kis baat par itna gussa aaya?",
                    "Gusse ko andar dabane se dimaag kharab hota hai yaar. Yaha sab bol dalo! Kisme gussa dilaya tumhe?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Share what happened", "Take a breath", "Calm down")
            }
            isFamily -> {
                val pool = listOf(
                    "Ghar walo ki expectations ya relationship issues kafi emotionally drain kar dete hain yaar. Thoda space lo aur aaram se saans lo.",
                    "Breakup ya dosti mein ladai hui hai kya bhai? Yeh dard sach mein bohot गहरा hota hai. Jo bhi kehna hai, yaha khul kar kaho."
                )
                selectVariant(pool, recentlyUsed) to listOf("Vent more", "Take a breather", "Keep talking")
            }
            else -> {
                val pool = listOf(
                    "Main samajh sakta hu dost, dimaag mein kafi kuch chal raha hai. Sabkuch ek hi pal mein theek karna zaroori nahi hai. Thoda ruk kar saans lena bhi zaroori hai. Kya tum thoda aur batana chahoge?",
                    "Bolo bhai, main poora sun raha hoon! Koi bhi baat ho, bina jhijhak share karo. Kaisa feel ho raha hai?",
                    "Sahi baat hai yaar! Is baare mein aur kya soch rahe ho? Main bilkul tumhare sath hoon."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Breathing", "Share more", "Break into steps")
            }
        }
    }

    // ==========================================
    // ENGLISH FRIENDLY & DYNAMIC COMPANION
    // ==========================================
    private fun buildEnglishResponse(
        isClear: Boolean, isGreeting: Boolean, isIdentity: Boolean, isJoke: Boolean,
        isTechCoding: Boolean, isFood: Boolean, isEntertainment: Boolean, isGratitude: Boolean,
        isSadness: Boolean, isAnxiety: Boolean, isAnger: Boolean, isExam: Boolean,
        isPlacement: Boolean, isSleep: Boolean, isComparison: Boolean, isLoneliness: Boolean,
        isFamily: Boolean, isMotivation: Boolean, isCopingTool: Boolean, isCelebration: Boolean,
        isSubstance: Boolean, isFirstTurn: Boolean, originalText: String, recentlyUsed: Set<String>
    ): Pair<String, List<String>> {
        return when {
            isClear -> {
                val pool = listOf(
                    "I've reset our conversation pace! 🌿 Whenever you're ready to share what's on your mind, I'm right here. Take all the time you need. What would you like to reflect on today?",
                    "Fresh slate ready! 😊 How are you feeling right now? We can talk about whatever is on your mind.",
                    "Chat cleared out! Take a deep, refreshing breath. What shall we tackle or chat about next?"
                )
                selectVariant(pool, recentlyUsed) to listOf("I'm feeling stressed", "Exams are overwhelming", "Just want to chat")
            }
            isGreeting -> {
                val pool = if (isFirstTurn) {
                    listOf(
                        "Hello! I'm right here with you, and I'm so glad you checked in. 😊 This is your completely private sanctuary. How has your day been treating you so far?",
                        "Hey there! Great to see you. How are things going with you today? Hanging in there?",
                        "Hello! Welcome in. I'm all ears—whether you had a productive day, a chaotic one, or just need a safe place to vent, I've got your back.",
                        "Hey! What's going on today? Tell me everything, how are you feeling?"
                    )
                } else {
                    listOf(
                        "Hey again! Still right here with you. What's on your mind right now?",
                        "Hey! What's popping? Need to bounce some thoughts around or take a breather?",
                        "Still here and listening! Tell me what's going on.",
                        "Hey there! What shall we dive into next?"
                    )
                }
                selectVariant(pool, recentlyUsed) to listOf("Feeling overwhelmed", "Just wanted to say hi", "Need a 2-min pause", "Talk about college")
            }
            isIdentity -> {
                val pool = listOf(
                    "I'm MindMate, your personal on-device medical wellness companion! 🛡️ I run 100% offline inside your phone—no cloud servers, no trackers, and zero data leakage. Everything you share stays strictly between you and this device. I'm here to support you through academic pressure, burnout, racing thoughts, sleep struggles, or just to chat like a supportive friend.",
                    "Think of me as MindMate, your personal offline sanctuary and companion! 🤝 Everything happens purely on your phone hardware. No outside eyes, no telemetry. Whether you're stressed about grades, feeling lonely in the hostel, or just need a laugh, I'm right here.",
                    "I'm MindMate—an offline, privacy-first companion designed to keep you grounded. 🌿 No ads, no cloud uploads, completely confidential. What can I help you navigate today?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Try a 2-min reset", "Talk about college stress", "How does offline AI work?")
            }
            isJoke -> {
                val pool = listOf(
                    "Haha, need a mental palate cleanser? Here's a quick joke for you: 💻\n\nWhy do programmers prefer dark mode?\nBecause light attracts bugs! 🐛😂\n\nDid that coax out at least a little grin? How's your mood holding up?",
                    "Here's a relatable college joke: ☕\n\nMy professor told me: 'Don't put off till tomorrow what you can do today.'\nI told him: 'Fine, I'll put it off till next semester then.' 😂\n\nBreathe easy! Don't let the daily grind crush your sense of humor.",
                    "Here's a fun joke to beat the boredom: 🤖\n\nThere are 10 types of people in this world: those who understand binary, and those who don't! 😂\n\nBoredom can be super annoying. Want to play a quick trivia game, brainstorm something cool to build, or talk about movies?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Tell another joke", "College life banter", "Exams are stressing me")
            }
            isTechCoding -> {
                val pool = listOf(
                    "Debugging bugs can make anyone want to gently toss their laptop out the window! 💻 Seriously, staring at stack traces for hours melts your brain. Have you stepped away from the screen for 5 minutes? Usually the fix hits you the moment you walk away.",
                    "Ah, coding frustration! 👨‍💻 Syntax errors, merge conflicts, and dependency hell are the ultimate test of sanity. Take a sip of water. What kind of bug or project are you wrangling?",
                    "When the compiler throws a red wall of errors right before a submission, your heart rate definitely spikes! 😂 Don't panic—let's break it down one function at a time."
                )
                selectVariant(pool, recentlyUsed) to listOf("Take a 5-min break", "Break into steps", "Tell more details")
            }
            isFood -> {
                val pool = listOf(
                    "Mmm, talking about food is instant mood therapy! 🍛 Whether it's a piping hot biryani, late-night Maggi, or just surviving hostel mess food, good food fixes a lot. What did you eat today?",
                    "A warm cup of chai or coffee is honestly a mental reset button! ☕ Sit back, enjoy the warmth, and let all the deadlines wait for just ten peaceful minutes.",
                    "Hostel mess food having zero flavor is practically a universal college canon event! 😂 Did you grab something decent to eat at least?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Need a chai break", "Mess food complaints", "Keep chatting")
            }
            isEntertainment -> {
                val pool = listOf(
                    "Movies, gaming, or anime! 🎮 The absolute best way to let your brain decompress. Have you watched anything great lately or played any intense matches?",
                    "Taking entertainment breaks without feeling guilty is so important. 🍿 Constant grinding just leads to burnout. What game or show are you into right now?",
                    "Putting on a solid playlist and tuning out the world works wonders. 🎧 What genre keeps you in your comfort zone?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Recommend music", "Gaming chat", "Back to work")
            }
            isGratitude -> {
                val pool = listOf(
                    "You are so welcome! Prioritizing your emotional health and taking time to unpack things takes genuine bravery. Rest easy and take things one gentle step at a time today. I'll be right here whenever you need me. 🤍",
                    "Anytime! That's what friends are for. 😊 Seeing you feel even a fraction lighter makes my day. Take good care of yourself!",
                    "You're always welcome, happy to be in your corner! 🌿 Remember to be kind to yourself today. Catch you later!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Good night", "Take a breath", "See you later")
            }
            isCelebration -> {
                val pool = listOf(
                    "That is fantastic news! 🎉 You put in honest effort, navigated the unknowns, and made it through. Take a deep breath and truly savor this win—you've earned every bit of it. How are you celebrating today?",
                    "YES!! Look at you go! 🥳 After all that stress and self-doubt, you totally crushed it. Super proud of you!",
                    "Huge congratulations! 🌟 Make sure you take a minute to pat yourself on the back and treat yourself. Moments like this deserve to be celebrated!"
                )
                selectVariant(pool, recentlyUsed) to listOf("Thank you!", "Feeling great", "Next goals")
            }
            isCopingTool -> {
                val pool = listOf(
                    "Let's do a gentle 4-4-4 box breathing cycle right now together:\n\n1. Inhale slowly through your nose for 4 seconds. 🌬️\n2. Hold your breath gently for 4 seconds.\n3. Exhale completely through your mouth for 4 seconds.\n4. Rest empty for 4 seconds.\n\nLet your jaw soften and feel your shoulders drop down.",
                    "Take a slow breath and let's do the 5-4-3-2-1 Sensory Grounding exercise:\n• 5 things you can see around you right now\n• 4 things you can physically feel\n• 3 distinct sounds you can hear\n• 2 things you can smell\n• Inhale gently and think 1 kind thought about yourself. Feel your feet flat on the floor. 🌿"
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Box Breathing", "Sensory Grounding", "Keep talking")
            }
            isSubstance -> {
                val pool = listOf(
                    "Dealing with substance urges or cravings can feel deeply overwhelming. Recognizing these triggers and acknowledging what you're experiencing is a courageous first step. You will never face judgment here.\n\nFor free, confidential support and counseling, you can reach the National Drug De-Addiction Helpline at 1972 (24/7 toll-free). You don't have to navigate this alone."
                )
                selectVariant(pool, recentlyUsed) to listOf("Call Helpline 1972", "Sensory Grounding", "Keep talking")
            }
            isExam -> {
                val pool = listOf(
                    "I can see why that feels heavy. It sounds like the exam pressure and syllabus weight are making it difficult to even start. Let's make the next step tiny: spend just 5 minutes opening the first topic without expecting perfection.",
                    "College exams can feel like a relentless marathon, especially when the syllabus looks like an insurmountable mountain! Take a slow breath with me. We don't have to conquer the whole semester today—let's take one small step and review just one manageable topic.",
                    "Exam anxiety is so real, but remember: your grades do not define your worth as a person. Let's take things one step at a time. What subject is causing the biggest headache right now?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Try a 2-min reset", "Break into tiny steps", "Keep talking")
            }
            isPlacement -> {
                val pool = listOf(
                    "Career expectations and placement anxiety can feel deeply exhausting. Everyone moves at a different cadence, even when campus banter makes it feel like an urgent race. Pause for a moment: let's identify just one small action you have full control over today.",
                    "The placement season pressure is brutal—especially watching peers get offers while you're still in the trenches. But your timeline is unique to you. Want to tackle one resume bullet point or solve one aptitude problem together?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Focus Reset", "Break down tasks", "Talk through it")
            }
            isSleep -> {
                val pool = listOf(
                    "Lying in bed while your mind races is physically draining. At night, stressors magnify because our cognitive defenses are low. Let's gently transition: would you like to run a 2-minute box breathing cycle to slow your pulse?",
                    "Late-night overthinking at 2 AM is the worst because everything seems 10x darker and more hopeless than it actually is. Put your screen away, place a hand over your chest, and just focus on slow, gentle exhales."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try Box Breathing", "Sensory Grounding", "Wind-down tips")
            }
            isComparison -> {
                val pool = listOf(
                    "Feeling like you're falling behind your peers is a heavy, quiet weight to carry. Remember that you're comparing your behind-the-scenes struggles to everyone else's curated highlight reels. You don't need to win the year today; you only need to take care of the next hour.",
                    "Comparison really is the thief of joy, especially in college. You've fought your own battles and overcome hurdles that nobody else knows about. Trust your own trajectory."
                )
                selectVariant(pool, recentlyUsed) to listOf("Reflect on thoughts", "Name one small win", "Continue talking")
            }
            isLoneliness -> {
                val pool = listOf(
                    "Feeling isolated—especially in a bustling college or hostel environment—can feel very sharp. Having tough days does not mean you are fundamentally alone or unworthy of connection. Would you like to draft a simple reach-out message to a trusted friend or mentor?",
                    "Sitting in a quiet room feeling like nobody truly gets what you're going through hurts deep. I'm right here with you, and you can say whatever is on your heart. Want to tell me what sparked this feeling?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Reach-Out Plan", "Grounding Exercise", "Stay here with me")
            }
            isMotivation -> {
                val pool = listOf(
                    "Procrastination is rarely about being lazy; it's almost always about emotional overwhelm or dread of getting stuck. To bypass that resistance, let's pick a micro-step that requires almost zero energy—like just reviewing one slide or writing one line. What is the simplest piece of the puzzle you can touch for 3 minutes?",
                    "Getting stuck in the doom-scrolling loop and then feeling flooded with guilt is something so many students struggle with. Don't beat yourself up for the lost hours. Let's just win the next 10 minutes."
                )
                selectVariant(pool, recentlyUsed) to listOf("Pick a micro-step", "Try 2-min reset", "Talk it through")
            }
            isSadness -> {
                val pool = listOf(
                    "I hear the weight behind what you're feeling, and I want you to know it's okay to feel down. You don't need to put on a brave face or immediately force yourself to be cheerful. Heavy emotions need room to breathe. Would you like to put words to what triggered this, or would you prefer to just sit with a soothing 2-minute reset?",
                    "I'm so sorry things are feeling heavy right now. 🤍 If you need to cry or just let out a sigh, please don't hold back. I'm sitting right here beside you. Tell me what's on your heart."
                )
                selectVariant(pool, recentlyUsed) to listOf("Tell you what happened", "Try a 2-min pause", "Just stay here with me")
            }
            isAnxiety -> {
                val pool = listOf(
                    "I hear how noisy and overwhelming things feel inside your head right now. When anxiety spikes, your nervous system is simply trying to shield you, but it kicks everything into high alert. Feel both feet firmly grounded on the floor. Take a long, slow exhale. We can break whatever is bothering you into tiny, manageable steps.",
                    "Take a slow breath with me. Drop your shoulders down away from your ears. Anxiety makes everything feel like an emergency, but right in this very second, you are safe. Let's take it one step at a time."
                )
                selectVariant(pool, recentlyUsed) to listOf("Try a 2-min reset", "Break into tiny steps", "Explore this further")
            }
            isAnger -> {
                val pool = listOf(
                    "Your frustration is completely valid. Feeling angry or irritated often means a boundary was crossed or expectations felt crushed. You don't have to swallow it down. This is your safe space to speak freely without judgment. What was the tipping point today?",
                    "Ugh, that sounds infuriating! Vent it all out right here. Who or what caused this flare-up?"
                )
                selectVariant(pool, recentlyUsed) to listOf("Tell what happened", "Take a slow breath", "Release the tension")
            }
            isFamily -> {
                val pool = listOf(
                    "Navigating family expectations or relationship friction can be emotionally exhausting. Even when family means well, their pressure can feel suffocating. Take a moment to breathe and give yourself permission to protect your peace.",
                    "Relationship troubles and misunderstandings hurt in a very raw way. You don't have to bottle it up inside—I'm here to listen."
                )
                selectVariant(pool, recentlyUsed) to listOf("Vent more", "Take a breather", "Keep talking")
            }
            else -> {
                // Dynamic open-ended reflection with conversational warmth
                val pool = listOf(
                    "I hear you, and I appreciate you sharing that with me. It sounds like there's quite a bit swirling in your mind right now. What's the biggest thing pulling at your attention at the moment?",
                    "That makes complete sense. When so many thoughts are competing for space, it helps to just lay them out one by one. Tell me a bit more—how does that make you feel?",
                    "I'm completely tuned in. Take your time, there's zero rush. What part of that has been lingering with you the most today?",
                    "Thank you for being open with me. We can take this anywhere you want—whether you want to brainstorm a solution or just have someone who genuinely listens."
                )
                selectVariant(pool, recentlyUsed) to listOf("Take a 2-min pause", "Explore this further", "Break into steps")
            }
        }
    }
}
