package fr.loupgarou.multi

/** Écriture JSON minimale (le moteur n'a aucune dépendance). Les valeurs acceptées :
 *  null, String, Boolean, Number, List<*>, Map<String, *>. */
object Json {
    fun ecrire(valeur: Any?): String = StringBuilder().also { ecrire(valeur, it) }.toString()

    private fun ecrire(v: Any?, sb: StringBuilder) {
        when (v) {
            null -> sb.append("null")
            is String -> chaine(v, sb)
            is Boolean, is Int, is Long -> sb.append(v.toString())
            is Number -> sb.append(v.toDouble().toString())
            is Map<*, *> -> {
                sb.append('{')
                var premier = true
                for ((k, x) in v) {
                    if (!premier) sb.append(',')
                    premier = false
                    chaine(k.toString(), sb)
                    sb.append(':')
                    ecrire(x, sb)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                v.forEachIndexed { i, x ->
                    if (i > 0) sb.append(',')
                    ecrire(x, sb)
                }
                sb.append(']')
            }
            else -> chaine(v.toString(), sb)
        }
    }

    private fun chaine(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' || c == '<' || c == '>' || c == '&' -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }
}
