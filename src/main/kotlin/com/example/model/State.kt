package com.example.model

import java.util.concurrent.CopyOnWriteArraySet

data class State(val key: String) {
    companion object {
        private val registry = CopyOnWriteArraySet<State>()
        val ADD_GROUP_NAME = register("add_group_name")

        val ADD_EX_NAME = register("add_ex_name")
        val ADD_EX_GROUP = register("add_ex_group")
        val SHOW_GROUPS = register("show_groups")
        val SHOW_EXERCISES = register("show_exercises")
        val SHOW_DIFFICULTY = register("show_difficulty")

        val CHOOSE_USER_EX_GROUP = register("choose_user_ex_group")
        val CHOOSE_USER_EX = register("choose_user_ex")
        val ADD_USER_EX_NORMAL_DIFFICULTY = register("add_user_ex_normal_difficulty")
        val CHOOSE_TRAINING_GROUP = register("choose_training_group")
        val CHOOSE_TRAINING_EX = register("choose_training_ex")
        val SET_TRAINING_DIFFICULTY = register("set_training_difficulty")
        val SET_TRAINING_REPEATS = register("set_training_repeats")
        val SET_TRAINING_GROUPED = register("set_training_grouped")

        private fun register(key: String): State {
            val name = State(key)
            registry.add(name)
            return name
        }
    }
}