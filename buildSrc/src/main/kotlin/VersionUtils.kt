/**
 * Extension function to convert version string (X.Y.Z) to version code
 * Formula: major * 10000 + minor * 100 + patch
 * Example: "1.2.3" -> 10203
 */
fun String.toVersionCode(): Int {
    val parts = this.split(".")
    require(parts.size == 3) { "Version must be in format X.Y.Z" }
    val (major, minor, patch) = parts.map { it.toInt() }
    return major * 10000 + minor * 100 + patch
}
