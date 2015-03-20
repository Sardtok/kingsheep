# Komme i gang #

Installer Apache Ant om du ikke har det fra før.

Rediger build.xml og sett inn navnet på laget ditt i variabelen TEAMNAME.

Kildemappen heter kingsheep. I undermappen team lager du en kopi av awesome-mappen som heter det samme som laget ditt. Endre pakkenavnet i klassene Sheep og Wolf i denne mappen til «kingsheep.team.teamname».

  * ant compile - kompiler prosjektet
  * java -jar dist/kingsheep.jar test.map teamname awesome - konkurrer mot awesome på testkartet.

# Om koordinater #

Koordinatsystemet er satt opp etter matriser med rader først og kolonner etterpå. Altså: **`map[y][x]`**

Dette er motsatt av hva man vanligvis gjør i spill. Det er dessverre litt sent å endre på dette nå, så de av dere som begynner opp litt sent må dessverre leve med dette.

# Testing med egne kart #

Kartformatet er enkelt. 15 linjer med 19 tegn. Punktum `'.'` gir tomme ruter, `'g'` gir gress, `'r'` gir rabarbra og `'#'` gir gjerder/skigard. I tillegg er det fire spesielle ruter: 1, 2, 3 og 4. Disse setter startstedet til de forskjellige spillernes dyr. 1 og 3 er sauene og 2 og 4 er ulvene.

Hvis du lagrer kartene i res-mappa, kan du kjøre en kompilering og spille kartet ved å kjøre programmet som angitt ovenfor. Om du legger det et annet sted, må du kjøre programmet litt annerledes:
```
Linux/Mac:
java -cp .:dist/kingsheep.jar kingsheep.KingSheep mymap.map teamname awesome

Windows:
java -cp .;dist\kingsheep.jar kingsheep.KingSheep mymap.map teamname awesome
```
-cp angir at man skal oppgi en liste med mapper og jar-arkiver der program- og ressursfiler ligger (CLASSPATH). Her setter vi det til mappa vi kjører programmet fra og kingsheep.jar i undermappa dist. Så oppgir vi hvilken klasse vi skal kjøre (klassen med main-metoden) som er KingSheep i pakken kingsheep. Da kan vi bruke kartet `'mymap.map'` som ligger i mappa vi kjører King Sheep fra.