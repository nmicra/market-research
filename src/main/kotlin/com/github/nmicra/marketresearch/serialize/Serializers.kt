package com.github.nmicra.marketresearch.serialize

import com.github.nmicra.marketresearch.analysis.Indicator
import com.github.nmicra.marketresearch.general.BIGDECIMAL_SCALE
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeDouble(value.toDouble())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeDouble()).setScale(BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
    }
}

object IndicatorsSerializer : KSerializer<Indicator> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Indicator) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Indicator {
        return Indicator.allIndicators.firstOrNull() { it.rawName() == decoder.decodeString() } ?: error("Indicator [${decoder.decodeString()}] does not exist")
    }
}

object MovingAvgMapSerializer: KSerializer<MutableMap<Int, BigDecimal>> {
    private val mapSerializer = MapSerializer(Int.serializer(), BigDecimalSerializer)

    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: MutableMap<Int, BigDecimal>) {
        mapSerializer.serialize(encoder, value.toMutableMap())
    }

    override fun deserialize(decoder: Decoder): MutableMap<Int, BigDecimal> {
        return mapSerializer.deserialize(decoder).toMutableMap()
    }
}