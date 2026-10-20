# Punteggio – tennis e padel per Wear OS

App per smartwatch Wear OS (pensata per il OnePlus Watch 2R) per tenere il punteggio mentre si gioca a tennis o a padel.

## Cosa fa

- Punti 0 / 15 / 30 / 40 / AD, oppure **golden point** (padel) sul 40-40.
- Game, set e **tie-break** a 7 sul 6-6, con la rotazione corretta del servizio.
- **Super tie-break** a 10 opzionale per il set decisivo.
- Set unico, 2 su 3 o 3 su 5.
- **Annulla** l'ultimo punto (anche più volte, anche dopo la fine di un set o della partita).
- Indicatore di chi serve (pallina gialla).
- La partita viene salvata a ogni punto: se l'app si chiude, riprende da dove eri.
- Lo schermo resta acceso durante la partita.

## Come si usa

1. Scegli sport, numero di set, golden point, super tie-break e chi serve, poi **Inizia**.
2. Tocca la **metà alta** (blu, "LORO") o la **metà bassa** (verde, "NOI") per assegnare il punto.
3. Al centro vedi set e game; il tasto ↶ annulla l'ultimo punto.

## Struttura

- `core/` – logica del punteggio in Kotlin puro, con test (`./gradlew :core:test`).
- `wear/` – app Wear OS in Jetpack Compose.

## Compilare e installare sull'orologio

Serve [Android Studio](https://developer.android.com/studio) (oppure scarica l'APK già compilato dalla scheda **Actions** del repo, artefatto `punteggio-debug-apk`).

1. Sull'orologio: Impostazioni → Sistema → Informazioni → tocca 7 volte "Numero build" per attivare le Opzioni sviluppatore.
2. Opzioni sviluppatore → attiva **Debug ADB** e **Debug wireless**. Orologio e computer devono essere sulla stessa rete Wi-Fi.
3. In Debug wireless tocca "Associa nuovo dispositivo" e sul computer:
   ```
   adb pair <ip>:<porta-associazione>   # inserisci il codice mostrato sull'orologio
   adb connect <ip>:<porta>
   ```
4. Da Android Studio premi Run scegliendo l'orologio, oppure:
   ```
   ./gradlew :wear:installDebug
   # o con l'APK scaricato:
   adb install wear-debug.apk
   ```
