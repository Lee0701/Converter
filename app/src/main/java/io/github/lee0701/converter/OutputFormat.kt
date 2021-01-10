package io.github.lee0701.converter

sealed class OutputFormat {

    abstract fun getOutput(hanja: String, hangul: String): String
    operator fun invoke(hanja: String, hangul: String): String = getOutput(hanja, hangul)

    companion object {
        fun of(name: String): OutputFormat? {
            return FORMATS[name]
        }

        private val FORMATS = mapOf<String, OutputFormat>(
            "hanja_only" to HanjaOnly(),
            "hanjahangul" to HanjaWithHangul(),
            "hanja_hangul" to HanjaWithParenthesisedHangul(),
            "hangul_hanja" to HangulWithParenthesisedHanja(),
        )
    }

    class HanjaOnly: OutputFormat() {
        override fun getOutput(hanja: String, hangul: String): String {
            return hanja
        }
    }

    class HanjaWithHangul: OutputFormat() {
        override fun getOutput(hanja: String, hangul: String): String {
            return if(hanja == hangul) hanja else "$hanja$hangul"
        }
    }

    class HanjaWithParenthesisedHangul: OutputFormat() {
        override fun getOutput(hanja: String, hangul: String): String {
            return if(hanja == hangul) hanja else "$hanja($hangul)"
        }
    }

    class HangulWithParenthesisedHanja: OutputFormat() {
        override fun getOutput(hanja: String, hangul: String): String {
            return if(hanja == hangul) hanja else "$hangul($hanja)"
        }
    }
}