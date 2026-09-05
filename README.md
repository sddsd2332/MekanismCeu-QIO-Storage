# Mekanism CEU QIO Storage

QIO storage integrations for Mekanism CE Unofficial on Minecraft 1.12.2. The
addon adds dedicated Mekanism QIO drives and transfer adapters for resources
provided by supported magic, power and technology mods.

## Requirements

- Minecraft 1.12.2
- Forge 14.23.5.2768 or newer
- Mekanism CE Unofficial

Thaumcraft, Botania, Blood Magic, Astral Sorcery, Embers, Nature's Aura and
PneumaticCraft: Repressurized are optional. Each integration is registered only
when its Forge mod ID is loaded:

| Mod | Mod ID | QIO resources |
| --- | --- | --- |
| Thaumcraft | `thaumcraft` | Essentia, one QIO type per Aspect tag |
| Botania | `botania` | Mana |
| Blood Magic | `bloodmagic` | Soul Network LP; five Demon Will types |
| Astral Sorcery | `astralsorcery` | Starlight |
| Embers | `embers` | Ember |
| Nature's Aura | `naturesaura` | Aura |
| PneumaticCraft: Repressurized | `pneumaticcraft` | Air |

The addon uses Mekanism's public extensible QIO API. It does not modify the
Mekanism source tree, use reflection to reach optional APIs, or require JEI for
resource storage. Optional provider jars are compile-only dependencies for
development; Forge loading checks keep integrations isolated when a provider is
absent.

## QIO drives and units

Every supported resource has four specialized drive tiers matching Mekanism's
`base`, `hyper_dense`, `time_dilating` and `supermassive` tiers. Single-resource
integrations use a one-type drive. Blood Magic Will drives share their type
capacity across all five Will variants. Native fractional remainders are kept
in the source when an integration stores whole QIO units.

The unit shown by each drive is:

- Thaumcraft: `1` unit = `1` Essentia; each Aspect is an independent resource.
- Botania: `1` unit = `1` Mana.
- Blood Magic LP: `1` unit = `1` Soul Network LP, bound to one owner UUID.
- Blood Magic Will: `1` unit = `1` Will. The drive supports raw/default,
  corrosive, destructive, vengeful and steadfast Will, with shared capacity.
- Astral Sorcery: `1` unit = `1` altar Starlight point. One native Astral
  Sorcery network amount equals 200 QIO units; constellation is output metadata,
  not a separate stored type.
- Embers: `1` unit = `1` Ember; fractional native Ember remains in the source.
- Nature's Aura: `1` unit = `1` Aura.
- PneumaticCraft: `1` unit = `1` air. Pressure is calculated by the native
  handler as air divided by its volume.

## Integration behavior

### Thaumcraft Essentia

Transfers use Thaumcraft's `IAspectSource`, `IAspectContainer` and
`IEssentiaTransport` APIs. Adjacent compatible transport faces, containers and
machines can import or export Essentia. Aspect tags are persisted as stable QIO
resource identities, so different Aspects never share a drive entry.

### Botania Mana

Transfers use Botania's Mana pool and compatible receiver APIs. Pool capacity,
creative pools, sided capability checks and simulation are respected. QIO
selection can resolve charged Mana items and populated Mana drives.

### Blood Magic LP and Will

LP is stored per Blood Magic Soul Network owner UUID. LP transfer uses native
Soul Network permissions and simulation, keeping each owner's network separate.

Will transfer uses native `IDemonWillConduit` and `IDemonWillGem` APIs. Adjacent
Demon Crucibles, Demon Pylons and Demon Crystallizers are supported, as are Will
items, crystals, Tartaric Gems and populated Will drives. All five native Will
types are selectable and share the dedicated Will drive's capacity.

### Astral Sorcery Starlight

Starlight is stored as one generic resource independent of constellation. The
QIO importer can receive from the native Astral Sorcery network, while the
exporter sends through its normal output face to adjacent `IStarlightReceiver`
targets. Altars and ritual pedestals can receive output; altar focus crystals
and pedestal crystals provide constellation metadata without creating separate
QIO resource types. No Linking Tool binding or Astral Sorcery source
registration is required.

### Embers Ember

Transfers use Embers' `IEmberCapability` on the target face. The adapter honors
native capacity and simulation while storing whole Ember units and leaving
fractional remainders in the provider. The QIO GUI uses Embers' native
`particle_star` visual style.

### Nature's Aura

Transfers use the sided `IAuraContainer` capability. The adapter checks the
world Aura type and the container's accepted type before inserting, and honors
native capacity and simulation for extraction and insertion.

### PneumaticCraft Air

Transfers use PneumaticCraft's `IAirHandler` through `IPneumaticMachine`. QIO
stores the handler's non-negative integer air amount; pressure remains a native
handler calculation based on air and volume. Capacity uses the handler's maximum
pressure and volume, and extraction/insertion preserves the provider's actual
post-transfer amount. The QIO GUI uses PneumaticCraft's native air particle
texture.

## QIO GUI rendering

Client-side resource renderers are registered with the same optional-mod gates
as the transfer adapters. The GUI uses the provider's familiar visuals: native
Aspect glyphs for Thaumcraft, Botania's mana-water sprite, Blood Magic's LP and
Will item renderers, Astral Sorcery's `astralsorcery:itemshiftingstar`, Embers'
`embers:entity/particle_star` particles, Nature's Aura's aura-cache texture and
PneumaticCraft's animated `air_particle.png` particles.

## Development

The authoritative Mekanism API dependency is
`libs/Mekanism-CE-Unofficial-All-10.0.4.250-dev.jar`.

```powershell
.\gradlew.bat compileJava --no-daemon
.\gradlew.bat test --no-daemon
```

Optional integration smoke checks run in dedicated worlds under `run`, stop the
server or client automatically, and write reports and logs to
`build/reports/qio-smoke`:

```powershell
.\gradlew.bat runServer --no-daemon -Pqio_smoke_test=true
.\gradlew.bat runServer --no-daemon -Pqio_smoke_test=true -Pqio_smoke_reload=true
.\gradlew.bat runServer --no-daemon -Pqio_smoke_test=true -Pqio_smoke_integration=will
.\gradlew.bat runClient --no-daemon -Pqio_smoke_test=true
```

To verify the class-loading path without optional provider jars, add
`-Penable_optional_mod_runtime=false` to a development task. Smoke-test classes
are development-only and are not included in the published mod.

The Chinese documentation is available in [README_zh_cn.md](README_zh_cn.md).
