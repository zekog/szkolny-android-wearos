<div align="center">

![Readme Banner](.github/readme-banner.png)

[![Discord](https://img.shields.io/discord/619178050562686988?color=%237289DA&logo=discord&logoColor=white&style=for-the-badge)](https://szkolny.eu/discord)
[![Oficjalna strona](https://img.shields.io/badge/-website-orange?style=for-the-badge&logo=internet-explorer&logoColor=white)](https://szkolny.eu/)
[![Facebook Fanpage](https://img.shields.io/badge/-facebook-blue?style=for-the-badge&logo=facebook&logoColor=white)](https://szkolny.eu/facebook)

![Wersja Androida](https://img.shields.io/badge/android-4.4%2B-orange?style=for-the-badge&logo=android)
[![Najnowsza wersja](https://img.shields.io/github/v/release/szkolny-eu/szkolny-android?color=%2344CC11&include_prereleases&logo=github&logoColor=white&style=for-the-badge)](https://github.com/szkolny-eu/szkolny-android/releases/latest)
![Licencja](https://img.shields.io/github/license/szkolny-eu/szkolny-android?color=blue&logo=github&logoColor=white&style=for-the-badge)

[![Release build](https://img.shields.io/github/actions/workflow/status/szkolny-eu/szkolny-android/release.yml?label=Release&logo=github-actions&logoColor=white&style=for-the-badge)](https://github.com/szkolny-eu/szkolny-android/actions/workflows/release.yml)
[![Play build](https://img.shields.io/github/actions/workflow/status/szkolny-eu/szkolny-android/push-master.yml?label=Play&logo=google-play&logoColor=white&style=for-the-badge)](https://github.com/szkolny-eu/szkolny-android/actions/workflows/push-master.yml)
[![Nightly build](https://img.shields.io/github/actions/workflow/status/szkolny-eu/szkolny-android/schedule-dispatch.yml?label=Nightly&logo=github-actions&logoColor=white&style=for-the-badge)](https://github.com/szkolny-eu/szkolny-android/actions/workflows/schedule-dispatch.yml)

</div>

## To jest prywatny fork

To repozytorium jest **prywatnym forkiem** projektu [szkolny-eu/szkolny-android](https://github.com/szkolny-eu/szkolny-android). Zawiera ono dodatkową aplikację **Szkolny na Wear OS** (moduły `:wear` oraz `:wear-data`) i nie jest w żaden sposób powiązane z autorami oryginału. Linki do strony, Google Play i wydań w dalszej części odnoszą się do **oryginalnego** projektu.

## Aplikacja na Wear OS

Rozszerzenie oryginalnej aplikacji o zegarki z **Wear OS 5+**. Telefon nadal odpowiada za logowanie i synchronizację z e-dziennikiem, a zegarek tylko wyświetla dane — dzięki temu na małym ekranie nie trzeba wpisywać hasła ani przechodzić przez logowanie Librus&reg;.

### Funkcje na zegarku

- **Plan lekcji** (priorytet): wybór dnia, zastępstwa, zmiany i lekcje przeniesione, lekcje odwołane, sala i nauczyciel
- **Oceny** — grupowane po przedmiocie wraz ze średnią
- **Zadania i wydarzenia** — posortowane po dacie
- **Frekwencja** — podsumowanie (obecne/nieobecne/usprawiedliwione/spóźnienia) oraz lista
- **Uwagi** — pochwały, uwagi, punkty
- **Wiadomości** — lista, treść i załączniki
- **Ogłoszenia** — lista i szczegóły
- **Szczęśliwe liczby**
- **8 motywów** kolorystycznych, w tym Catppuccin (Latte, Frappé, Macchiato, Mocha) oraz motyw ciemny, zielony i fioletowy
- **Obsługa koronki** (przewijanie list)

### Jak to działa

1. Aplikacja na telefonie loguje się do e-dziennika i po każdej synchronizacji publikuje dane **aktywnego profilu** (plan, oceny, zadania, frekwencję, uwagi, wiadomości, ogłoszenia, szczęśliwe liczby).
2. Moduł `:wear` pobiera je z telefonu i zapisuje w lokalnym cache, więc działa też offline.
3. Transport danych:
   - najpierw **LAN** — `UDP discovery` + `TCP` (jeden request `/szkolny/bundle` dla wszystkich sekcji),
   - a jeśli się nie uda — **Wearable Data Layer**.
4. Na telefonie działa usługa pierwszego planu `WearSyncService`, która utrzymuje serwer LAN przy życiu (cicha, stała notyfikacja).

### Wymagania

- Zegarek z **Wear OS 5+** (minSdk 30) sparowany z telefonem.
- Telefon i zegarek w **tej samej sieci Wi-Fi**.
- Oba APK podpisane **tym samym kluczem** i z tym samym `applicationId` (wersja debug: `pl.szczodrzynski.edziennik.debug`).
- Aplikacja na telefonie uruchomiona przynajmniej raz (startuje wtedy usługa). Zalecane wyłączenie optymalizacji baterii dla aplikacji, aby usługa nie była zabijana.

### Budowanie i instalacja

```bash
./gradlew :app:assembleUnofficialDebug
./gradlew :wear:assembleDebug

adb -s <telefon> install -r app/build/outputs/apk/unofficial/debug/app-unofficial-debug.apk
adb -s <zegarek> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

Moduły:

- `:app` — aplikacja na telefon (zawiera `WearSyncManager` i `WearLanServer`)
- `:wear` — aplikacja na zegarek (Jetpack Compose for Wear OS)
- `:wear-data` — współdzielone modele DTO i serializacja JSON między telefonem a zegarkiem

### Rozwiązywanie problemów

- **Brak danych na zegarku** — otwórz aplikację na telefonie (uruchomi usługę i serwer), upewnij się, że oba urządzenia są w tej samej sieci Wi-Fi. Przycisk „Odśwież" na zegarku wymusza ponowne pobranie.
- **Niektóre zegarki OPPO/OnePlus** (parowane przez OHealth, bez aplikacji Google Wear) nie replikują danych przez Wearable Data Layer — dlatego podstawowym kanałem jest LAN.
- **Koronka** — jeśli przewijanie działa w odwrotnym kierunku lub jest zbyt wolne/szybkie, zmień znak w `WearRotary.onRotate` lub wartość `WearRotary.STEP_PIXELS`.

## Ważna informacja

Jak zapewne już wiecie, we wrześniu 2020 r. **firma Librus zabroniła nam** publikowania w sklepie Google Play naszej aplikacji z obsługą dziennika Librus&reg; Synergia. Prowadziliśmy rozmowy, aby **umożliwić Wam wygodny, bezpłatny dostęp do Waszych ocen, wiadomości, zadań domowych**, jednak oczekiwania firmy Librus zdecydowanie przekroczyły wszelkie nasze możliwości finansowe. Mając na uwadze powyższe względy, zdecydowaliśmy się opublikować kod źródłowy aplikacji Szkolny.eu. Liczymy, że dzięki temu aplikacja będzie mogła dalej funkcjonować, być rozwijana, pomagając Wam w czasie zdalnego nauczania i przez kolejne lata nauki.

__Zachęcamy do [przeczytania całej informacji](https://szkolny.eu/informacja) na naszej stronie.__

*- Autorzy Szkolny.eu*

## O aplikacji

Szkolny.eu jest nieoficjalną aplikacją, umożliwiającą rodzicom i uczniom dostęp do danych z e-dziennika w każdym smartfonie. Jest to jedyna aplikacja, która posiada wsparcie dla wszystkich najpopularniejszych e-dzienników. Oznacza to, że mając kilka kont w różnych szkołach, wystarczy mieć tylko jedną aplikację.

### Funkcje aplikacji

- plan lekcji, terminarz, oceny, wiadomości, zadania domowe, uwagi, frekwencja
- wygodne **widgety** na ekran główny
- łatwa komunikacja z nauczycielami — **odbieranie, wyszukiwanie i wysyłanie wiadomości**
- pobieranie **załączników wiadomości i zadań domowych**
- **powiadomienia** o nowych informacjach na telefonie lub na komputerze
- organizacja zadań domowych i sprawdzianów — łatwe oznaczanie jako wykonane
- obliczanie **średniej ocen** ze wszystkich przedmiotów, oceny proponowane i końcowe
- Symulator edycji ocen — obliczanie średniej z przedmiotu po zmianie dowolnych jego ocen
- **dodawanie własnych wydarzeń** i zadań do terminarza
- nowoczesny i intuicyjny interfejs użytkownika
- **obsługa wielu profili** uczniów — jeżeli jesteś Rodzicem, możesz skonfigurować wszystkie swoje konta uczniowskie i łatwo między nimi przełączać
- opcja **automatycznej synchronizacji** z E-dziennikiem
- opcja Ciszy nocnej — nigdy więcej budzących Cię dźwięków z telefonu

[Zobacz porównanie funkcji z innymi aplikacjami](https://szkolny.eu/funkcje)

### Pobieranie

Najnowsze wersje możesz pobrać z Google Play lub bezpośrednio z naszej strony, w formacie .APK.

[<img src=".github/google-play-badge.png" height="100px">](https://szkolny.eu/pobierz/android)
[<img src=".github/apk-badge.png" height="100px">](https://szkolny.eu/pobierz)

### Kompilacja

Aby uruchomić aplikację „ze źródeł” należy użyć Android Studio w wersji co najmniej 4.2 Beta 6. Wersja `debug` może wtedy zostać zainstalowana np. na emulatorze Androida.

Aby zbudować wersję produkcyjną, tzn. `release` należy użyć wariantu `mainRelease` oraz podpisać wyjściowy plik .APK sygnaturą w wersji V1 i V2.

Warianty `play` oraz `official` są zastrzeżone dla wydań oficjalnych.

Aplikację na zegarek buduje się niezależnie:

```bash
./gradlew :wear:assembleDebug      # lub :wear:assembleRelease
```

Wersja `release` zegarka jest podpisywana kluczem debug tylko jako domyślny fallback — aby synchronizacja przez Wearable Data Layer działała, telefon i zegarek muszą być podpisane **tym samym** kluczem.

## Współpraca

PRy wprowadzające nowe funkcje lub naprawiające błędy są mile widziane!

__Jeśli masz jakieś pytania, zapraszamy na [nasz serwer Discord](https://szkolny.eu/discord).__

## Licencja

Szkolny.eu publikowany jest na licencji [GNU GPLv3](LICENSE). W szczególności, deweloper:
- Może modyfikować oraz usprawniać kod aplikacji
- Może dystrybuować wersje produkcyjne
- Musi opublikować wszelkie wprowadzone zmiany, tzn. publiczny fork tego repozytorium
- Nie może zmieniać licencji ani copyrightu aplikacji

Dodatkowo:
- Zabronione jest modyfikowanie lub usuwanie kodu odpowiedzialnego za zgodność wersji produkcyjnych z licencją.

- **Wersje skompilowane nie mogą być dystrybuowane za pomocą Google Play oraz żadnej platformy, na której istnieje oficjalna wersja aplikacji**.

**Autorzy aplikacji nie biorą odpowiedzialności za używanie aplikacji, modyfikowanie oraz dystrybuowanie.**

Znaki towarowe zamieszczone w aplikacji oraz tym dokumencie należą do ich prawowitych właścicieli i są używane wyłącznie w celach informacyjnych.
