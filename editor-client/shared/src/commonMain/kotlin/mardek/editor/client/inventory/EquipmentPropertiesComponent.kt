package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch
import mardek.editor.client.component.BitserIntField
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun EquipmentPropertiesComponent(equipment: BitClient.Struct) {
	val mutateScope = rememberCoroutineScope()
	var canSave by remember { mutableStateOf(false) }
	var canSaveWeapon by remember { mutableStateOf(false) }
	var nullableWeapon by remember { mutableStateOf<BitClient.Struct?>(null)}

	Row {
		nullableWeapon?.let { weapon ->
			WeaponPropertiesComponent(weapon)
			DisposableEffect(weapon) {
				val subscription = weapon.subscribeCanSave { mutateScope.launch { canSaveWeapon = it } }
				onDispose {
					weapon.cancelSubscription(subscription)
				}
			}
		}
		BitserIntField(equipment.getSimpleField(null, "charismaticPerformanceChance"))
		Button(onClick = {
			equipment.save()
			nullableWeapon?.save()
		}, enabled = canSave || canSaveWeapon) {
			Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
		}
	}

	DisposableEffect(equipment) {
		equipment.start()
		val weaponSubscription = equipment.subscribeChildStruct(null, "weapon") {
			mutateScope.launch { nullableWeapon = it }
		}
		val canSaveSubscription = equipment.subscribeCanSave { mutateScope.launch { canSave = it } }
		onDispose {
			equipment.cancelChildStructSubscription(null, "weapon", weaponSubscription)
			equipment.cancelSubscription(canSaveSubscription)
			equipment.close()
		}
	}
}
