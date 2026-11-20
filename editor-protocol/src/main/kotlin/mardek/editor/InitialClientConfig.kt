package mardek.editor

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import java.io.Serializable
import java.util.UUID

@BitStruct(backwardCompatible = false)
class InitialClientConfig(

	@BitField
	val serverIp: String,

	@IntegerField(expectUniform = true, minValue = 0)
	val serverPort: Int,

	@NestedFieldSetting(path = "", writeAsBytes = true)
	val serverCertificate: ByteArray,

	@BitField
	val initialPassword: UUID,
) : Serializable
