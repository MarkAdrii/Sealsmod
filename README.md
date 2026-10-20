# Seals

Mod de Minecraft en el que sellas mobs dentro de ti para obtener sus poderes ("seal" = sello en inglés).

- **Minecraft:** 1.20.1
- **Forge:** 47.4.10 o superior
- **Java:** 17

## Mods necesarios para jugar

Además de Seals, instala en la carpeta `mods`:

- [Lodestone](https://www.curseforge.com/minecraft/mc-mods/lodestone) para Forge 1.20.1 (efectos visuales)
- [Player Animator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) para Forge 1.20.1 (animaciones)

En desarrollo (`runClient`) se descargan solos.

## Cómo se juega

1. Craftea un **Pergamino de sello**: papel + saco de tinta + perla de ender.
2. Baja la vida de un **Iron Golem** al 25% o menos y haz clic derecho sobre él con el pergamino.
3. Pulsa **U** para abrir el vínculo: afinidad, estabilidad, edad, estrellas y habilidades.
4. Pulsa **Alt** para el modo combate y usa las teclas **1-9** para lanzar habilidades.

### Iron Golem

| Nivel | Habilidad | Qué hace |
|---|---|---|
| Al sellarlo | Manto ferruquímico | Piel de hierro brillante, +20 de armadura, sin empuje |
| ★ | Pies de hierro | Sin daño por caída |
| ★★ | Yo soy el golem | Te haces más grande, +10 de armadura, +6 de daño a mano desnuda |
| ★★★ | Holy golem | Invoca un golem espiritual azul, rápido y que salta |

Todo escala con la edad del golem (20-100 años al sellarlo; 1 día de Minecraft = 1 mes).

- **Afinidad:** tradeos +1, criaturas hostiles +1, bosses +50, usar habilidades +1/min, cada año +70.
- **Estabilidad:** matar aldeanos -10, jugadores -20, aliados (mascotas, golems, compañeros de equipo) -5. A 0 se rompe el sello y sale un golem furioso con el doble de vida.

## Compilar

```bash
./gradlew build
```

El `.jar` queda en `build/libs/`. Cada push a GitHub también se compila solo en la pestaña **Actions**.

## Probar en desarrollo

```bash
./gradlew genIntellijRuns
./gradlew runClient
```

En Windows usa `.\gradlew` en lugar de `./gradlew`.

## Añadir un mob nuevo

1. Crea una clase que extienda `SealDefinition` (mira `seal/irongolem/IronGolemSeal.java`).
2. Regístrala en `SealRegistry`.
3. Añade sus textos en `lang/es_es.json` y `lang/en_us.json`, y sus iconos en `textures/gui/ability/`.
