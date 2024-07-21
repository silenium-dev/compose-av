package dev.silenium.multimedia.data

data class Rational(val num: Long, val den: Long) {
    init {
        require(den != 0L) { "Denominator must not be zero" }
    }

    val asDouble: Double get() = num.toDouble() / den

    operator fun times(other: Rational) = Rational(num * other.num, den * other.den).minimal
    operator fun div(other: Rational) = Rational(num * other.den, den * other.num).minimal
    operator fun plus(other: Rational) = Rational(num * other.den + other.num * den, den * other.den).minimal
    operator fun minus(other: Rational) = Rational(num * other.den - other.num * den, den * other.den).minimal
    operator fun unaryMinus() = Rational(-num, den)

    private val minimal: Rational by lazy {
        val gcd = gcd(num, den)
        Rational(num / gcd, den / gcd)
    }
}

private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

operator fun Long.times(other: Rational) = Rational(this, 1) * other
operator fun Int.times(other: Rational) = Rational(this.toLong(), 1) * other
