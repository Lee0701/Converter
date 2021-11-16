package io.github.lee0701.converter

sealed class OutputFormat {

    abstract fun getOutput(hanja: CharSequence, hangul: CharSequence): CharSequence
    operator fun invoke(hanja: CharSequence, hangul: CharSequence): CharSequence = getOutput(hanja, hangul)

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
        override fun getOutput(hanja: CharSequence, hangul: CharSequence): CharSequence {
            return hanja
        }
    }

    class HanjaWithHangul: OutputFormat() {
        override fun getOutput(hanja: CharSequence, hangul: CharSequence): CharSequence {
            return if(hanja == hangul) hanja else "$hanja$hangul"
        }
    }

    class HanjaWithParenthesisedHangul: OutputFormat() {
        override fun getOutput(hanja: CharSequence, hangul: CharSequence): CharSequence {
            return if(hanja == hangul) hanja else "$hanja($hangul)"
        }
    }

    class HangulWithParenthesisedHanja: OutputFormat() {
        override fun getOutput(hanja: CharSequence, hangul: CharSequence): CharSequence {
            return if(hanja == hangul) hanja else "$hangul($hanja)"
        }
    }
}