package furhatos.app.eyetracking

import furhatos.app.eyetracking.flow.Init
import furhatos.skills.Skill
import furhatos.flow.kotlin.Flow
import furhatos.nlu.LogisticMultiIntentClassifier

class EyetrackingSkill : Skill() {
    override fun start() {
        Flow().run(Init)
    }
}

fun main(args: Array<String>) {
    LogisticMultiIntentClassifier.setAsDefault()
    Skill.main(args)
}
