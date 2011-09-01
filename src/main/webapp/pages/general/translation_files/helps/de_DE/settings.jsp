<div style="page-break-after: always;">
<p class="head_description">
Die Einstellungen in Cyclos definieren jede Konfiguration auf Systemebene.
</p>
<i>Wo ist es zu finden?</i><br>
Die Einstellungen sind erreichbar über &quot;Menü: Einstellungen&quot;.<br>
Die folgenden Einstellungstypen gibt es: 
<ul>
<li><b><a href="#local"><u>Basiseinstellungen:</u></a></b> Jede lokale Besonderheit eines Systems, 
wie Sprache, Zeitzone und Format. 
<li><b><a href="#alerts"><u>Meldungseinstellungen:</u></a></b> Grenzwerte und andere Einstellungen in 
Zusammenhang mit Systemmeldungen. 
<li><b><a href="#access"><u>Zugang:</u></a></b> Einstellungen in Zusammenhang mit Zugang und Sicherheitsstufen. 
<li><b><a href="#mail"><u>E-Mail:</u></a></b> E-Mail Servereinstellungen.
<li><b><a href="#log"><u>Protokolle:</u></a></b> Einstellungen der Protokolldateien. 
<li><b><a href="#channels"><u>Kommunikationswege:</u></a></b> Konfiguration der Kommunikationswege (Zugangs-Medien).
<li><b><a href="#web_services_clients"><u>Web-Services-Clients:</u></a></b> Konfiguration der Webservices.
<li><b>Systemaufträge:</b> Es gibt zwei Typen von Systemaufträgen. <b><a
		href="#search_indexes"><u> Indexe verwalten</u></a></b> and <b> <a
		href="#online_state"><u> Systemverfügbarkeit </u></a></b>
<li><b><a href="#import_export"><u>Import / Export:</u></a></b> Einstellungen für Import- und Export.
</ul>
<hr>

<A NAME="local"></A>
<h2>Basiseinstellungen</h2>
Diese Einstellungen hängen ab von Ort und System. Um Änderungen vorzunehmen, klicken Sie 
bitte auf die Schaltfläche &quot;Bearbeiten&quot;. Um die Veränderungen zu speichern, klicken 
Sie bitte auf &quot;Weiter&quot;.
<p> 
<b>Cyclos-Instanz Identifizierung</b>
<ul>
	<li><b>Anwendungsbezeichnung:</b>
	Die ist die Bezeichnung der Anwendung, so wie er in der Titelanzeige Ihres Browsers erscheint. 
	<li><b>Anwendung Benutzername:</b>
	Dieser Name wird für automatisch generierte Nachrichten vom System an das Mitglied verwendet. 
	Zum Beispiel: Die &quot;Cyclos Administration&quot; hat Ihre Rechnung über ... akzeptiert. 
	<li><b>Identifizierung für externe Kommunkitationswege:</b>
	Dieser Name wird verwendet, wenn eine Software einer Drittpartei auf Cyclos zugreift, und die 
	Instanz identifizieren muss, mit der sie sich verbindet. Dies wäre normalerweise der 
	Instanzname. 
	<li><b>Allgemein Containerseite URL:</b> 
	Dieses Feld wird benötigt, wenn Sie von einer Website (wie das Demo auf der Cyclos Projektseite) 
	auf Cyclos zugreifen wollen. Die Website hat entweder einen Iframe oder einen Frameset, der die 
	Cyclos-Anmeldeseite beinhaltet. Falls Sie dies so tun wollen, sollten Sie vielleicht den 
	&quot;Top&quot; anpassen (im Content Management – Statische Dateien - top.jsp).<br>
	
	Bei der URL muss es sich um eine vollständige URL mit vorangehender http:// oder https:// handeln. 
	Bitte beachten Sie, dass wenn Sie eine URL-Anfrage an die normale Seite stellen, gelangen Sie 
	direkt auf die Containerseite. Wenn die Containerseite nicht richtig funktioniert, bedeutet das, 
	dass Sie mit der normalen URL nicht auf Cyclos zugreifen können. Wenn dies passiert, können Sie 
	auf Cyclos immer noch mit der originalen Anmeldeangabe nach der URL, auf Cyclos zugreifen.
  Zum Beispiel: http://www.yourdomain.org/cyclos/do/login<br>
	Cyclos unterstützt mehrfache Gemeinschaften in einer Instanz. Auf jede Gemeinschaft kann über eine 
	eigene (benutzerdefinierte) Anmeldeseite, oder von einer eigenen Website aus zugegriffen werden. 
	Um dies freizugeben, müssen Sie pro Gruppe oder Gruppenfilter eine Containerseite URL definieren. 
	Bei dieser Art von Konfiguration ist die globale Container URL der als Standard eingestellte 
	(Standard-Rückfall) Container für Gruppen, die keine eigene Website haben, mit der sie auf Cyclos 
	zugreifen können.<br>
	Mehr Information dazu finden Sie in den Einstellungen der <a href="${pagePrefix}groups#group_details">
	<u>Gruppen</u></a> und <a href="${pagePrefix}groups#group_filter"><u>Gruppenfilter</u></a>.
</ul>	
		
	<b>Internationalisierung</b>
<ul>
	<li><b>Sprache: </b>
	Verschiedene Sprachen stehen zur Auswahl. Zur Zeit sind sieben Sprachen verfügbar.
	<li><b>Zahlenformat: </b>
	Wählen Sie ein Format für die Komma/Punkt-Trennung von Zahlen. Zur Zeit unterstützen 
	wir die amerikanische und die europäische Zahlenformatierung. 
	<li><b>Zahlengenauigkeit: </b>
	Diese Einstellung regelt die Anzahl der Nachkommastellen (oder das Dezimalzeichen). Ist dies 
	auf Null gestellt, funktioniert die Anwendung nur mit ganzen Zahlen. Für die meisten Systeme 
	ist diese Einstellung bei 2.
	<li><b>Hohe Zahlengenauigkeit: </b>
	Diese Einstellung regelt die Anzahl der Nachkommastellen (oder das Dezimalzeichen), nur für 
	Kontogebühren. Normalerweise ist die Einstellung hier die gleiche wie bei der Zahlengenauigkeit. 
	In manchen Fällen kann es aber auch möglich sein, für die Berechnung der Kontogebühren einen höheren 
	Grad an Genauigkeit zu benötigen. 
	<li><b>Eingabemethode Dezimalzahlen: </b> 
	Mit dieser Einstellung definieren Sie, ob der Dezimaltrenner (z.B. das Komma) automatisch 
	eingegeben 	werden soll, was bedeutet, dass der Benutzer nur die Zahlen eingeben muss (erste 
	Option &quot;rechts nach links&quot;). Die zweite Option (&quot;links nach rechts&quot;) bedeutet, 
	dass der Benutzer auch den Dezimaltrenner eingeben muss. 
	<li><b>Zeitzone: </b> Die können Sie die Zeitzone definieren. Dies muss nur dann eingestellt werden, 
	wenn der Server sich in einer anderen Zeitzone befindet, als die Benutzer der Instanz. 
	<li><b>Datumsformat: </b>
	Wählen Sie hier das Format für das Datum, so wie es auf dem Bildschirm erscheinen soll.
	<li><b>Zeitformat: </b>
	Wählen Sie ein Zeitformat. Die Zeit erscheint vollständig mit Datum und Uhrzeit auf allen Überweisungen, 
	Meldungen, Rechnungen und Bemerkungen. 
</ul>

<b>Limits </b>
<ul>
	<li><b>Max. Ergebnisse pro Seite: </b>
	Die maximalen Ergebnisse (Zeilen) in allen Druckausgaben oder beim &quot;herunterladen&quot; als CSV-Datei.
	Diese Option ist gesetzt, um hohe Auslastungen auf dem Server zu vermeiden.
	<li><b>Max. Ergebnisse pro Seite: </b>
	Die maximale Anzahl von Elementen auf einer Suchergebnisseite. 
	<li><b>Max. Ajax-Ergebnisse: </b>
	Die maximale Anzahl von Elementen in den sich automatisch vervollständigenden Felder, 
	wie &quot;Gehe zum Mitgliedsprofil&quot; und direkten Zahlungen und Rechnungen. 
	<li><b>Maximale Upload-Größe: </b>
	Dies ist der maximale Dateigröße für Profil- und Inseratbilder. 
	<li><b>Maximale Bildgröße: </b>
	Dies ist die maximale Größe (Breite und Länge) der Bilder (z.B. für Inserate und Profile). 
	Sind die hochgeladenen Formate größer, wird Cyclos die Bilder automatisch verkleinern. 
	<li><b>Maximale Bildgröße Miniaturbild: </b>
	Dies ist die maximale Größe (Breite und Länge) der Vorschaubilder (anwählbare Bilder für Inserate 
	und Mitglieder). Das Vorschaubild hat die gleichen Dimensionen wie das Originalbild. Die Größe 
	kann daher kleiner sein, wenn das Bild im &quot;Hochformat&quot; ist. 
	<li><b>Brokering Ablauf: </b>
	Ist dieser Zeitraum eingestellt, verschwindet danach das registrierte Mitglied 
	eines Brokers von seiner Liste. Wenn Ihr System mit <a href="${pagePrefix}brokering#commission">
	<u>Brokerkommissionen</u></a> arbeitet, sollten Sie sicherstellen, dass der Kommissionszeitraum 
	nicht kürzer als die Broker-Ablauffrist ist. 
	<li><b>Lösche Nachrichten im Papierkorb nach: </b>
	Dies ist der maximale Zeitraum, während dessen sich Nachrichten im Papierkorb befinden. 
	Nach diesem Zeitraum wird die Nachricht entfernt. 
	<li><b>Maximale Zeit für E-Mail-Bestätigung bei Mitgliedsregistrierung: </b>
	Registriert sich ein Mitglied extern und ist E-Mail-Bestätigung aktiviert 
	(in Gruppeneinstellungen &quot;Bestätigung E-Mail-Adresse bei öffentlicher Registrierung&quot;), 
	muss das Mitglied innerhalb dieses Zeitraums bestätigen.  
</ul>

<b>Datenanzeige: </b>
<ul>
	<li><b>Verbindliches E-Mail für Mitglieder: </b> Ist diese Option gewählt, so ist das 
	E-Mail-Feld verpflichtend (z.B. bei Registrierung durch Administratoren, oder bei 
	öffentlicher Registrierung des Mitglieds). 
	<li><b>Mitglieder Ergebnisliste zeigt: </b> Mit dieser Einstellung definieren Sie, ob die 
	Listen mit den Suchergebnissen nach Mitgliedern und nach Inseraten den Mitgliedsnamen 
	oder den Benutzernamen zeigen. Gemeinschaftssysteme wie LETS verwenden für gewöhnlich den 
	Benutzernamen als &quot;Spitznamen&quot;, und ziehen es vor, diesen Namen in der Inserateliste 
	zu zeigen. In geschäftlichen Tausch-Netzwerken ist es üblicher, den Namen des Mitglieds 
	zu verwenden.
	<li><b>Inserat Beschreibungsformat:</b> 
  Hier wählen Sie das Format der Beschreibung (Text). Es kann sich dabei um ein normales 
  Textbereich oder um einen WYSIWYG-Editor (Rich-Texteditor) handeln (Was Sie sehen, ist das, was Sie bekommen). 
  Ebenso ist es möglich, dem Benutzer beide Optionen offen zu halten, und lediglich eine davon 
  als Standard einzustellen. 	
	<li><b>Einfaches Nachrichtenformat:</b> Hier bestimmen Sie den Editortyp für die normalen 
	Nachrichten zwischen Mitgliedern und Administratoren, und umgekehrt.
	<li><b>Gruppen Nachrichtenformat:</b> Hier definieren Sie den Editor für die Nachrichten 
	an Gruppen (nur für Administratoren). 
	<li><b>Nachrichtenformat Broker an registrierte Mitglieder: </b> Hier definieren Sie den Editor für 
	die Nachrichten von einem Broker an seine registrierten Mitglieder. 
	<li><b>Zähler (Anzahl) bei den Inseratekategorien anzeigen: </b> Wird diese Einstellung gewählt, 
	erscheint die Anzahl der Inserate neben den Inseratkategorien – Links (auf der Seite 
	&quot;Inserate durchsuchen&quot;, beim Browsen der Kategorien). Der Grund für die Verwendung 
	dieser Funktion, wäre z.B. eine Instanz mit unterschiedlichen Gruppen/Gemeinschaften, die als 
	isolierte Gruppen funktionieren, und sich daher gegenseitig nicht sehen können (und auch nicht 
	ihre Inserate).<br>
	Da die Zähler global sind, stellen sie stets die Anzahl aller Inserate innerhalb des Systems dar 
	(selbstverständlich für die jeweilige Kategorie). Dies könnte zu Verwirrung führen, da die Zähler 
	höhere Zahlen angäben als die Anzahl der Inserate in der Gemeinschaft.<br>
	Also ist es im Falle, wenn mehrere Gemeinschafte auf einer Instanz konfiguriert sind, besser, 
	die Zähler zu deaktivieren.
</ul>

<b>CSV Export / Import: </b>
<ul>
	<li><b>Überschrift anzeigen:</b> 
	Wird diese Option gewählt, dann zeigt die erste Zeile der <a
		href="${pagePrefix}loans#csv"><u>CSV-Datei</u></a> den Namen des Feldes 
	in der ersten Zeile jeder Spalte (zum Beispiel den Mitgliedsnamen, die Adresse usw.
	<li><b>Texterkennungszeichen: </b>
	Wird diese Option gewählt, dann werden die Felder vom Datentyp &quot;Zeichenkette&quot; 
	(String) wie 	Beschreibung oder Titel in Anführungszeichen gesetzt.<br>
  Bitte beachten: Wenn Sie bei dieser Option &quot;Keine&quot; stehen lassen, dann werden die 
  Zeichenketten, die Trennzeichen (wie Komma, Tabulator, oder Semikolon) enthalten, getrennt
  und erscheinen in der nächsten, also der falschen Spalte. Es ist daher sehr empfehlenswert,
  Zeichenketten in Anführungsstriche zu setzen.	
	<li><b>Trennzeichen: </b>
	Dies ist das Trennzeichen der CSV-Datei. Möglicherweise müssen Sie dieses Zeichen spezifizieren, 
	wenn Sie die Datei in eine Tabelle oder in ein Textverarbeitungsprogramm importieren möchten.
	<li><b>Zeilenumbruch: </b>
  Die ist das Zeichen, das ein Ende einer Zeile markiert. Der eingestellte UNIX Umbruch ist der 
  gebräuchlichste Standard.	Der &quot;DOS&quot;-Typ kann für Windows-Systeme verwendet werden. 
</ul>

<b>SMS: </b><p>
Wenn Sie SMS aktivieren, erlaubt Cyclos Zahlungen, Rechnungen und das Einholen von 
Kontoinformationen per SMS. Das SMS-Modul von Cyclos benötigt einen externen SMS-Controller. 
Diese Software befindet sich noch in der Entwicklung, und es gibt derzeit noch keine Pläne für 
eine Veröffentlichung. Es steht Ihnen frei, Ihren eigenen SMS-Controller zu verwenden.<br>
Die Kommunikation zwischen Cyclos und SMS-Controller läuft über Web-Services. Daher müssen Sie 
eine Web-Service-URL zur Verfügung stellen. Außerdem müssen Sie einen Kommunikationsweg für 
SMS-Zahlungen hinzufügen (in &quot;Menü: Einstellungen – Kommunikationswege&quot;). Ist diese 
SMS-Option erst einmal aktiv, erscheint sie in den Gruppeneinstellungen und als ein 
Kommunikationsweg in den Benachrichtigungseinstellungen. 
<p>
<b>Überweisungsnummer: </b>
<ul>
	<li><b>Überweisungsnummer: </b>
	Ist diese Option aktiviert, so generiert jede Überweisung im System eine einmalige 
	Überweisungsnummer (Identifikator). Das Format dieses Identifikators kann in den 
	folgenden Feldern definiert werden: 
	<ul>
		<li><b>Präfix: </b>
		Vorsilbe der Überweisungsnummer (Zahlen oder Buchstaben) 
		<li><b>Länge der Nummer: </b>
		Die Länge des Nummer (sequenziell) 
		<li><b>Suffix: </b>
		Nachsilbe des Überweisungsnummer (Zahlen oder Buchstaben) 
	</ul>
	Zum Beispiel: Die Überweisungsnummer der ersten Überweisung im System, mit der 
	Konfiguration Präfix=abc, Länge=5, Suffix=xyz ist demzufolge &quot;abc00001xyz&quot; 	
</ul>


<b>Rückbelastung: </b>
In dieser Einstellung definieren Sie die maximale Zeitspanne in der ein Administrator 
eine Zahlung rückbelasten darf. Rückbelastung bedeutet eine Zahlung in die umgekehrte 
Richtung. Hat die Zahlung andere Überweisungen generiert (wie z.B. Gebühren und Darlehen), 
so verursachen alle Überweisungen umgekehrte Überweisungen. Der maximale Zeitraum für 
die Rückbelastung einer Zahlung kann ebenfalls eingestellt werden. Die Beschreibung der 
generierten umgekehrten Überweisung kann die Variablen #date und #description enthalten, 
wobei es sich bei der Beschreibung um die Beschreibung der ursprünglichen Überweisung handelt.
<p>
Seien Sie bitte sorgfältig bei der Rückbelastung einer Überweisung mit dazugehörigen Darlehen. 
Dies kann zu Fehlern führen, wenn das Darlehen bereits zurück bezahlt wurde. 
<p>
<b>Geplante Aufgaben: </b> Diese Einstellung dient der besseren Ausführung. Über diese 
Einstellung bestimmen Sie, wann eine Aufgabe ausgeführt wird. Dies ist vor allem dann hilfreich, 
wenn mehr als eine Cyclos-Instanz auf dem gleichen System laufen. Cyclos beinhaltet sowohl 
stündliche als auch tägliche geplante Aufgaben. 
<ul>
	<li><b>Geplante Aufgaben, Stunde: </b> Hier stellen Sie die Stundenzahl (0-24) für die 
	täglichen geplanten Aufgaben ein. Ein Beispiel für eine tägliche Aufgabe ist die 
	Überprüfung abgelaufener Inserate. 
	<li><b>Geplante Aufgaben, Minute: </b> Hier stellen Sie die Minutenzahl (0-60) für die 
	täglichen und stündlichen geplanten Aufgaben ein (bei den täglichen Aufgaben werden die 
	Minuten den oben konfigurierten Stunden angefügt). Ein Beispiel für eine stündliche Aufgabe 
	ist die Überprüfung der Kontogebühren. 
	<li><b>Alle Suchindexe neu erzeugen: </b> Mit dieser Einstellung definieren Sie Dauer und 
	Häufigkeit, mit der Cyclos-Indexe neu aufgebaut werden. In Cyclos 3.5 sind die Mitglieder 
	und Inserate indexiert, was Suchen schneller laufen lässt. Die Indexierung ermöglicht auch 
	Mehrfach-Schlüsselwort-Suchen, und die Suche auf allen Feldern (Mitgliedsprofile oder 
	Inseratfelder)<br>
	Da allerdings Indexe mit der Zeit schadhaft werden können, ist es eine gute Idee, häufiger 
	Neuindexierungen vorzunehmen. Wir schlagen vor, die Neuindexierungen auf wöchentlicher 
	Basis und zu einer stillen Stunde vorzunehmen (nachts, oder früh am Morgen). Je nach Anzahl 
	der Inserate und der Mitgliedsprofile kann das einige Zeit in Anspruch nehmen. Der Vorgang 
	läuft in einem anderen Thread, und beeinträchtigt das Funktionieren von Cyclos nicht.<br>
  Die Neuindexierung kann aber auch dadurch geschehen, dass man die Indexe beim Server manuell 
  entfernt (einfach das Verzeichnis &quot;indexes&quot; im WEB-INF entfernen), und den Server 
  oder die Instanz neu starten. 
	<p>
</ul>
<hr>

<A NAME="alerts"></A>
<h2>Meldungseinstellungen</h2>
In den Meldungseinstellungen definieren Sie die Limits und die Grenzwerte für Meldungen, die 
im Zusammenhang mit dem Verhalten eines Mitglieds stehen. Um Veränderungen vorzunehmen, 
klicken Sie bitte auf die Schaltfläche &quot;Bearbeiten&quot;. Um die Änderungen zu speichern, 
klicken Sie bitte auf &quot;Weiter&quot;.<br> 
Zur Zeit können die folgenden Grenzwerte für Meldungen eingestellt werden: 
<ul>
	<li><b>Neuer Interessent: </b> Wird diese Option gewählt, wird jedes mal, wenn sich 
	ein neuer Benutzer registriert (auf der Anmeldeseite) eine Kontomeldung generiert. 
	<li><b> Erteilte, sehr schlechte Referenzen: </b> Hat jemand mehr als &quot;x&quot; sehr 
	schlechte Referenzen erteilt, wird eine Meldung generiert. 
	<li><b>Erhaltene, sehr schlechte Referenzen: </b> Hat jemand mehr als &quot;x&quot; sehr 
	schlechte Referenzen erhalten, wird eine Meldung generiert. 
	<li><b>Rechnungsablauf: </b> Erhält jemand eine Rechnung, reagiert aber nicht darauf (weder 
	durch Akzeptieren noch durch Annehmen), wird nach &quot;x&quot; Tagen eine Meldung ausgelöst. 
	<li><b>Zurückgewiesene Rechnungen: </b> Hat jemand mehr als &quot;x&quot; Rechnungen abgelehnt, 
	wird eine Meldung generiert. 
	<li><b>Ungültige Anmeldeversuche: </b> Nach &quot;x&quot; ungültigen Anmeldeversuchen wird eine 
	Meldung generiert. Existiert der Benutzer tatsächlich, wird eine Mitgliedsmeldung generiert. 
	Existiert das Mitglied nicht, wird eine Systemmeldung generiert. 
</ul>
<hr>

<A NAME="access"></A>
<h2>Zugangs-Einstellungen</h2>
Hier definieren Sie die Einstellungen für den Zugang zu Cyclos. Um Änderungen vorzunehmen, klicken Sie 
bitte auf die Schaltfläche &quot;Bearbeiten&quot;. Um die Veränderungen zu speichern, klicken Sie 
bitte auf &quot;Weiter&quot;.<br>
Die folgenden Optionen sind verfügbar: 
<ul>
	<li><b>Virtuelle Tastatur für Kennwort: </b> Ist diese Option angeklickt, erscheint für Benutzer 
	(sowohl für Administratoren als auch für Mitglieder) eine virtuelle Tastatur auf der Anmeldeseite. 
	Das Kennwort muss mit der virtuellen Tastatur eingegeben werden. Eine virtuelle Tastatur verhindert 
	dass böswillige Keylogger-Programme das Kennwort &quot;ausspionieren&quot;. 
	<li><b>Numerisches Kennwort: </b> Wird diese Option gewählt, so können Mitglieder nur 
	numerische Kennwörter haben. Diese Option ist vielleicht notwendig, wenn Mitglieder 
	auch Zahlungen mit Karten und PIN-Nummern vornehmen. Die Einstellung bezieht sich 
	nicht auf Administratoren. 
	<li><b>Operatoranmeldung erlauben:</b> Falls Sie  <a href="${pagePrefix}operators"><u>
	Operatoren</u></a> aktiviert haben, muss dieses Kästchen ausgefüllt werden, damit sich 
	die Operatoren anmelden können. 
	<li><b>Administratorsitzung Zeitüberschreitung: </b> Die Zeitspanne nach der ein 
	inaktiver Administrator abgemeldet wird. 
	<li><b>Mitgliedersitzung Zeitüberschreitung: </b> Die Zeitspanne nach der ein 
	inaktives Mitglied abgemeldet wird. 
	<li><b>POS-Web-Sitzung Zeitüberschreitung: </b>  Die Zeitspanne nach der ein 
	inaktives Mitglied oder Operator von der POS-Web-Seite abgemeldet wird. 
	<li><b>Zulassungsliste Administratorzugang: </b> Hier können Sie die IP-Adressen oder 
	Hostnamen der Benutzer eingeben, die Zugang zum Administrationsabschnitt haben. 
	Bitte schreiben Sie jeden Hostnamen oder IP-Adresse in eine neue Zeile (Eingabetaste). 
	Wenn Sie den &quot;#Any host&quot;-Eintrag leer lassen, hat jeder Host Zugang zur 
	Anmeldeseite der Administration (Benutzernahme und Kennwort werden allerdings immer 
	noch benötigt). 
	<li><b>Generierung Benutzername: </b>
	<ul>
		<li><b>Manuell für Mitglied: </b> Für Gemeinschaftsnetzwerke ist es üblich, dass Benutzer 
		ihren eigenen Benutzernamen oder &quot;Spitznamen&quot; wählen. In diesem Fall wählen 
		Sie bitte die Option &quot;Manuell für Mitglied&quot;.<br>
		Wird diese Option gewählt, können Sie die minimale und die maximale Länge 
		und einen regulären Ausdruck eingeben, um das Format des Benutzernames zu 
		bestimmen.<br>
		\. Alle Zeichen (mit oder ohne Übereinstimmung vom Zeilenende)<br>
		\d eine Ziffer: [0-9]<br> 
		\D keine Ziffer: [^0-9]<br>
		\s ein nicht druckbares Zeichen: [ \t\n\x0B\f\r]<br> 
		\S kein nicht druckbares Zeichen: [^\s]<br> 
		\w ein Wort-Zeichen: [a-zA-Z_0-9]<br>
		\W kein Wort-Zeichen: [^\w]<br>
		Informationen zu regulären Audrücken finden Sie unter:<br>
		http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.htm<br>
		Es ist möglich, die minimale und die maximale Länge des Benutzernamens festzulegen, 
		der von einem Mitglied oder Administrator verwendet werden kann. 
		<li><b>Generierte Zufallsnummer (Benutzer-Nummer): </b> Für geschäftliche Netzwerke ist 
		es üblich, generierte Benutzernamen oder &quot;Kontonummern&quot; zu verwenden. Ist dieses 
		Modul aktiviert, haben die Registrierungsformulare kein Feld für die Eingabe des 
		Benutzernamens. Wird das Formular ausgefüllt, wird automatisch ein Benutzername (Zufallsnummer) 
		generiert. Unter dieser Option können Sie die Länge des generierten Benutzercodes 
		spezifizieren. 
	</ul>
	<li><b>Mögliche Zeichen im Überweisungskennwort: </b> Hier können Sie definieren, welche 
	Zeichen für die Erstellung des Überweisungskennworts verwendet werden (in zufälliger Anordnung). 
	Für das Überweisungs-Kennwort – Einstellungen wenden Sie sich bitte an den Abschnitt 
	<a href="${pagePrefix}groups#edit_member_group"><u>Gruppeneinstellungens</u></a >.
</ul>
<hr>

<A NAME="mail"></A>
<h2>E-Mails-Einstellungen</h2>
Auf dieser Seite können Sie die E-Mail-Optionen einstellen. 
Definieren Sie hier die &quot;Absenderadresse&quot; und die SMTP Server Parameter. 
Falls Ihr Server TLS benötigt, müssen Sie die TLS-Option anklicken.<br> 
Um Änderungen vorzunehmen, klicken Sie bitte auf die Schaltfläche &quot;Bearbeiten&quot;. 
Um die Änderungen zu speichern, klicken Sie bitte auf &quot;Weiter&quot;. 
<hr>

<A NAME="log"></A>
<h2>Protokoll-Einstellungen</h2>
Die Protokoll-Einstellungen bestimmen, wo und wie protokolliert wird. Die Cyclos Protokoll-Dateien 
werden nicht in die Datenbank geschrieben, sondern in Textdateien auf dem Server. Es ist daher wichtig, 
dass diese Einstellung vom Server-Administrator verwaltet wird (oder in Zusammenarbeit mit diesem).<br> 
Um Änderungen vorzunehmen, klicken Sie bitte auf die Schaltfläche &quot;Bearbeiten&quot;. Um die Änderungen 
zu speichern, klicken Sie bitte auf &quot;Weiter&quot;.
<p>
Die folgenden Konfigurationen sind möglich: 
<ul>
	<li><b>Ereignisprotokoll-Level: </b>
	Dieses Protokoll enthält Informationen zu allen Cyclos-Aktionen, mit vollständiger Information 
	(Ablaufverfolgung) zur Aktion, wie Datum, Mitglied, etc.<br>
	Die folgenden Optionen sind möglich: 
	<ul>
		<li><b>Aus: </b>
		Ereignisprotokoll nicht in Verwendung. 
		<li><b>Nur Fehler: </b>
		Ereignisprotokoll enthält nur Fehler. 
		<li><b>Einfach Ablaufverfolgung (keine Parameter): </b>
		Ereignisprotokoll sammelt alle erfolgreichen und fehlerhaften Aktionen. Es zeigt den zeitlichen
		Verlauf der Funktionsaufrufe.
		<li><b>Detailierte Ablaufverfolgung (detailierte Parameter):</b>
		Wie oben, zeigt aber auch die übergebenen und zurückgegebenen Werte (Parameter). 
	</ul>
	<li><b>Ereignisprotokoll Datei-Pfad/Name: </b>
	Hier spezifizieren Sie den Pfad und den Dateinamen. Ist auf dem Server mehr als eine Instanz 
	installiert, ist es gute Praxis für jede Instanz ein eigenes Anmelde-Verzeichnis zu führen. 
	<ul>
		<li>"/" der lokale Pfadnamen-Trenner 
		<li>"%t" das temporäre Verzeichnis vom System
		<li>"%h" der Werte der "user.home" Systemvariable
		<li>"%g" die Generierungsnummer zur Unterscheidung rotierender Protokolle. 
		<li>"%u" eine einmalige Zahl zur Beilegung von Konflikten 
	</ul>
	<li><b>Überweisungsprotokoll-Level: </b>
	Dieses Protokoll enthält nur Überweisungen mit allen dazugehörigen Informationen, wie von 
	Mitglied/System an System/Mitglied, Betrag, Datum, etc.<br>
	Die folgenden Optionen sind möglich: 
	<ul>
		<li><b>Aus: </b>
		Überweisungsprotokoll nicht in Verwendung.
		<li><b>Normal: </b>
		Wird diese Option gewählt, enthält dieses Protokoll  alle Überweisungen mit Datum, von 
		Mitglied/System an Mitglied/System, und den Betrag. 
		<li><b>Detailiert: </b>
		Diese Option ergibt die gleiche Information wie unter &quot;Normal&quot;, zuzüglich der 
		Beschreibung und dem Überweisungstyp der Überweisungen. 
	</ul>
	<li><b>Überweisungsprotokoll Datei-Pfad/Name</b> <br>
	Hier spezifizieren Sie den Pfad und den Dateinamen, genau wie oben unter Ereignisprotokoll 
	Datei-Pfad/Name (Erklärung oben). 
	<li><b>Kontogebührprotokoll-Level: </b>
	Dieses Protokoll gibt Auskunft über die Kontogebühren (Beiträge). Der Administrationsbereich 
	von Cyclos beinhaltet einen <a href="${pagePrefix}account_management#account_fee_history"><u>
	Kontogebühren-Verlauf</u></a>. Der Grund warum die Kontogebühren in den Protokollen beinhaltet 
	sein können, ist möglichst vollständige Protokolle all dessen was innerhalb von Cyclos geschieht 
	erstellen zu können (außerhalb der Datenbank). Weiterhin ist die Kontogebühren-Protokollierung 
	umfangreicher als der Kontogebührenverlauf im Administrationsbereich.<br>
	Die folgenden Optionen sind möglich: 
	<ul>
		<li><b>Aus:</b> Kontogebührprotokoll nicht in Verwendung.
		<li><b>Nur Fehler:</b> zeigt nur Probleme. 
		<li><b>Statusänderungen und Fehler:</b> Statusänderungen und erfolgreiche Kontogebühren werden aufgezeichnet
		<li><b>Detailiert:</b> Protokolliert alle Vorgänge. 
	</ul>
	<li><b>Kontogebührprotokoll Datei-Pfad/Name</b> <br>
	Hier spezifizieren Sie den Pfad und den Dateinamen, genau wie oben unter Ereignisprotokoll 
	Datei-Pfad/Name (Erklärung oben). 
	<li><b>Geplante Aufgabe, Protokoll-Level</b> <br>
	Das Protokoll für geplante Aufgaben enthält das Kontogebühren-Protokoll. Informationen zu geplanten 
	Kontogebühren finden Sie auch in der Funktion	<a href="${pagePrefix}account_management#account_fee_overview">
	<u>Kontogebühren verwalten</u></a>. Der Grund dafür, sie auch bei den Protokollen einzuschließen, ist der, 
	möglichst vollständige Protokolle all dessen was innerhalb von Cyclos geschieht, erstellen zu können 
	(außerhalb der Datenbank). Die folgenden Optionen sind möglich: 
	<ul>
		<li><b>Aus:</b> es findet keine Protokollierung statt.
		<li><b>Nur Fehler:</b> Zeigt nur Probleme 
		<li><b>Zusammengefasste Ausführung:</b> Enthält lediglich die Information, dass die 
		Aufgabe ausgführt wurde, mit Zeitangabe. 
		<li><b>Detailierte Ausführung:</b> Detaillierter Informationen zur Ausführung der Aufgabe. 
		(Bitte beachten Sie, dass dies sehr umfangreich sein kann, da eine viele Mitglieder 
		betreffende Kontogebühr jedesmal eine separate Überweisung darstellt.) 
	</ul>
	<li><b>Geplante Aufgabe, Protokoll Datei-Pfad/Name </b> <br>
	Hier spezifizieren Sie den Pfad und den Dateinamen, genau wie oben unter Ereignisprotokoll 
	Datei-Pfad/Name (Erklärung oben). 
	<li><b>Maximale Anzahl Dateien je Protokoll: </b>
	Hier können Sie die maximale Anzahl der Protokolldateien spezifizieren. Ist die maximale Anzahl 
	der Protokolldateien erreicht, wird die älteste Protokolldatei gelöscht, und eine neue eingerichtet. 
	Es empfiehlt sich, die Protokolldateien zu sichern bevor sie gelöscht werden. 
	<li><b>Maximale Dateigröße: </b>
	Ist die maximale Dateigröße erreicht, wird eine neue Datei eingerichtet. Je intensiver Sie protokollieren, 
	desto höher muss natürlich die maximale Dateigröße sein.
</ul>
<hr>

<A NAME="channels"></A>
<h2>Kommunikationswege (Channels)</h2>
Cyclos kann Anfragen aus verschiedenen Medien oder &quot;Kommunikationswege&quot; bearbeiten. 
Dabei kann es sich um interne (Cyclos) Kommunikationswege handeln, wie Internet, POS-Web, Mobiltelefon, 
oder um externe (Zahlungs)wege für den Zugang von Dritt-Software wie E-Handel (E-Commerce), 
ATM-Maschinen und POS-Geräten. 
</p>
Um es einfacher zu machen, neue Kommunikationswege hinzuzufügen, sind zukünftig alle 
Kommunikationswege dynamisch gelistet. Die existierenden Kommunikationswege sind intern, 
und für normale Cyclos-Konfigurationen ist es nicht nötig, hier etwas zu ändern.
<br>
Die folgenden (internen) Kommunikationswege sind verfügbar: 
<ul>
	<li><b>Internetzugang: </b>
	Dies ist der Hauptzugang über den Web-Browser. Typischerweise mit URL, wie  
	www.yourdomain.org/cyclos 
	<li><b>POS: </b> Dieser Kommunikationsweg wird für Hard- und Software POS-Geräte 
	benötigt. Der Kommunikationsweg verwendet die POS-Kennung um den POS zu identifizieren.
	Weitere Informationen über POS finden Sie in der Hilfe unter <a
		href="${pagePrefix}access_devices#POS"><u>Zugangs-Geräte</u></a>.	
	<li><b>POS-Web-Zahlungen: </b>
	Point-of-sale Zahlungen (Zahlungen des Konsumenten direkt am Ort des Verkaufs). Wird von 
	<a href="${pagePrefix}operators"><u>Operatoren</u></a> verwendet, oder direkt von Mitgliedern. 
	Zugriff dazu durch Zufügen von /posweb oder /operator nach der URL-Instanz. Zum Beispiel: 
	www.yourdomain.org/cyclos/operator.<br>
	<li><b>WAP 1 Zugang:</b> (wireless application protocol) wird normalerweise benutzt von älteren 
	Mobiltelefonen, die WAP 2 nicht unterstützen. Zugriff auf das Modul erhalten Sie durch Eingabe 
	von /wap nach der Domain. 
	<li><b>WAP 2 Zugang: </b> WAP 2 ermöglicht Web-Zugang über Mobiltelefone. 
	Zugriff auf das Modul erhalten Sie durch Eingabe von /mobile nach der Domain.  
	<li><b>Internetshop-Zahlungen: </b>
	Der Kommunikationsweg Internetshop-Zahlungen ermöglicht Zahlungen über E-Handelssoftware 
	(E-Commerce). 
</ul>
Interne Kommunikationswege können nur programmatisch hinzugefügt werden. Falls Sie einen neuen 
externen Kommunikationsweg für Zahlungen zulassen möchten, müssen Sie beim Hinzufügen des
Kommunikationsweges zu einem <a href="${pagePrefix}account_management#transaction_types"><u>
Überweisungstyp</u></a> hinzufügen (derjenige, der mit dem Kanal genutzt werden soll).  
Es empfiehlt sich, pro Kommunikationsweg nur einen Überweisungstyp zu haben.<br> 
Für die Mitgliedergruppen, die diesen Kommunikationsweg verwenden werden, muss der Kommunikationsweg 
in den <a href="${pagePrefix}groups#edit_member_group"><u>Gruppeneinstellungen</u></a> freigegeben 
werden. Zusätzlich benötigt die Gruppe die entsprechenden Berechtigungen um den Überweisungstyp 
ausführen zu können. 
<hr>

<A NAME="channels_detail"></A>
<h3>Kommunikationsweg (neu oder ändern)</h3>
Es ist unwahrscheinlich, dass Sie Kommunikationswege konfigurieren müssen. 
Für POS-Web, POS und benutzerdefinierte Kommunikationswege können Sie Benutzeridentifikation
defnieren.<br>
Für jeden anderen Kommunikationsweg können Sie die Bestätigung der Anmeldeinformationen
definieren. (Diese Option ist nicht für den Internetzugang verfügbar, da in diesem Fall 
dies in der Gruppe selbst definiert ist)
<hr>

<A NAME="web_services_clients"></A>
<h2>Web-Services-Clients</h2>
Hier können Sie definieren, welche externe Software über Web-Service Zugriff auf Cyclos hat, und 
Sie können definieren, auf welche Services Zugriff möglich ist. 
<p>
Sie können einen Web-Service bearbeiten, indem Sie das Bearbeiten-Symbol wählen. Löschen können Sie 
ihn durch Klicken des Löschen-Symbols. 
<br>
Wenn Sie einen neuen Web-Service-Client hinzufügen möchten, klicken Sie auf die Schaltfläche
&quot;Weiter&quot; neben &quot;Neuer Web-Service-Client&quot;. 
<hr>

<A NAME="web_services_clients_detail"></A>
<h3>Web-Service-Client einfügen/ändern</h3>
Hier können Sie einen neuen Web-Service-Client einfügen, oder einen bereits Bestehenden bearbeiten. 
Wenn Sie damit fertig sind, klicken Sie bitte auf die Schaltfläche &quot;Weiter&quot; um Ihre 
Änderungen zu speichern. Möchten Sie einen bestehenden Client ändern, klicken Sie bitte zuerst auf die 
Schaltfläche &quot;Bearbeiten&quot;, andernfalls können die Felder nicht bearbeitet werden. 
<ul>
	<li><b>Name: </b>
	Die können Sie den Namen spezifizieren. Dies dient lediglich internen Zwecken. 
	<li><b>Internet-Addresse: </b> 
	Hier können Sie die IP-Adresse oder Domain-Namen angeben (welcher zu einer IP-Adresse 
	aufgelöst wird), dem Zugriff auf den Web-Service gewährt wird.<br>
	Bitte beachten Sie, dass wenn Sie eine Verbindung zum Server über ISP möchten, wird die 
	gleiche IP-Adresse wahrscheinlich für andere Internetseiten verwendet (über Hostheader). 
	Dies bedeutet, dass all diese Seiten Zugang zur Webinstanz hätten. 
	Häufig ist die IP-Adresse, welche zu einer Domain aufgelöst wird eine andere IP-Adresse,
	mit der man mit der Internetseite verbunden wird.	In diesem Fall müssten Sie Ihren Provider 
	kontaktieren, um heraus zu finden welche IP (Bereich) verwendet wird, um externe Verbindungen 
	herzustellen.<br>
  Es ist ebenfalls möglich einen IP-Bereich zu spezifizieren. (z.B. 77.88.45.1-265). Bitte bedenken 
  Sie, dass Spezifizierung eines IP-Bereichs eine Sicherheitslücke darstellen kann. In Fällen mit 
  weniger kritischem Inhalt, wie der Ansicht von Inseraten, stellt dies kein Problem dar. 
  Geht es aber um Zahlungen oder Mitgliedsdaten, ist es vorzuziehen, nur eine einzige IP-Adresse 
  zuzulassen.<br>
	<li><b>Kommunikationsweg: </b> 
	Hier wählen Sie einen Kommunikationsweg. Das kann ein Kommunikationsweg sein, den Sie hinzugefügt haben, 
	oder einen &quot;eingebauten (build in)&quot; Kommunikationsweg für &quot;Internetshop-Zahlungen&quot;. 
	(Der Kommunikationsweg für Internetshop-Zahlung ermöglicht Zahlungen über E-Handels (E-Commerce); 
	mehr Information dazu finden Sie im Cyclos-Wiki, Abschnitt: Web-Services - Webshop). 
	<li><b>Beschränkt auf Mitglied: </b>
	Hier können Sie definieren, ob der Web-Service auf ein bestimmtes Mitglied beschränkt ist. Der Zugangstyp 
	richtet sich nach den Berechtigungen (siehe unten). 
	<li><b>HTTP Benutzername / Kennwort: </b>
	In diesem (optionalen) Feld können Sie einen Benutzernamen und ein Kennwort spezifizieren. 
	Jede vom Web-Service-Client getätigte http-Anfrage benötigt Authentifizierung durch Benutzernamen 
	und Kennwort.<br>
	Wenn Sie diese Option verwenden, empfiehlt es sich https freizugeben, so dass Benutzer/Kennwort 
	verschlüsselt sind. 
	<li><b>Anmeldedaten erforderlich: </b> Diese Option ist nur dann verfügbar, wenn
	ein Kommunikationsweg gewählt wurde und es keine Einschränkung für Mitglieder gibt. Diese
	Option ermöglicht die Durchführung von Zahlungen über externe Software, allerdings werden die Anmeldedaten
	verlangt. Typischerweise würde das bedeuten, dass das die Zahlung durchführende Mitglied die
	Anmeldedaten eingibt und die auf den Web-Service zugreifende Software
	diese Daten erfasst und zusammen mit den Zahlungsdaten 
	an den Web-Service weiterleitet.
	<li><b>Berechtigungen: </b>
	<ul>
		<li><b>Zahlungen tätigen: </b> Diese Option ist nur dann verfügbar wenn ein Kommunikationsweg 
		gewählt wurde. Der Service ermöglicht Zahlungen über (externe) Kommunikationswege. Sie können einen 
		oder mehrere Zahlungstypen wählen. Ist der Web-Service auf ein Mitglied beschränkt, so kann dieses Mitglied 
		über diesen Kommunikationsweg nur Zahlungen in Auftrag geben (aber keine empfangen). 
		<li><b>Zahlungen empfangen: </b> Diese Option ist nur dann verfügbar wenn ein Kommunikationsweg 
		(und auf ein Mitglied beschränkt) wurde. Der Service ermöglicht Zahlungen über (externe) Kommunikationswege. 
		Sie können einen oder mehrere Zahlungstypen wählen. Ist der Web-Service auf ein Mitglied beschränkt, 
		so kann dieses Mitglied über diesen Kommunikationsweg nur Zahlungen empfangen. Die Zahlung benötigt 
		einen Zahlungs-<a	href="${pagePrefix}passwords#pin"><u>PIN</u></a>.
		<li><b>Rückbelastung:</b> Diese Option ist nur dann verfügbar,
		wenn ein Kommunikationsweg gewählt wurde. Der Servie ermöglicht <a href="#local"><u>charge
		back payments</u></a> über (externe) Kommunikationswege für Zahlungen. Sie können einen oder 
		mehrere Zahlungstypen wählen.
		<li><b>Inserate suchen: </b> Ermöglicht Ihnen, nach Inseraten zu suchen (normalerweise innerhalb einer 
		Internetseite). Ist der Web-Service auf das Mitglied beschränkt, kann der Service nur solche Inserate 
		abrufen, die das Mitglied/die Mitgliedsgruppe zeigen kann.
		<li><b>Mitglieder suchen: </b> Ermöglicht Ihnen, nach Inseraten zu suchen (normalerweise innerhalb einer Internetseite). 
		Ist der Web-Service auf ein Mitglied beschränkt, kann der Service nur solche Inserate abrufen, die 
		das Mitglied/die Mitgliedsgruppe zeigen kann.
		<li><b>Internetshop Zahlungen: </b> Ermöglicht das Empfangen von Zahlungen durch Cyclos-Benutzer, 
		über Internetshop. Der Internetshop-Zahlungs-Kommunikationsweg hat nur eine einzige Berechtigung die 
		gewählt werden kann (Internetshop-Zahlungen). Authentifizierung des Mitglieds geschieht über das 
		Ticket-System. Eine Erklärung hierzu finden Sie auf der Cyclos Wiki-Seite > Web-Services  
		( <a href="http://project.cyclos.org/wiki/index.php?title=Web_services"><u>
		http://project.cyclos.org/wiki/index.php?title=Web_services</u></a>). 
		<li><b>Auf Kontodetails zugreifen: </b> Alle Suchen nach Kontodetails und Überweisungen. Ist der 
		Web-Service auf ein Mitglied beschränkt, kann der Service nur Überweisungen des Mitglieds abrufen.
	</ul></ul>
<hr>
<A NAME="search_indexes"></A>
<h2>Indexe verwalten</h2>
Indexe ermöglichen eine schnelle Suche nach Benutzern, Inseraten
und Mitgliedseinträgen. Die Indexe ermöglichen die Suche nach Schlagworten oder
einer Kombination von Schlagworten. Die Indexe sind nicht in der Datenbank gespeichert, sondern
in eigenen Dateien auf dem Server. Werden beim Starten von Cyclos keine Indexe gefunden,
generiert ein Initialisierungsvorgang die Indexe. Nach einer gewissen
Zeit (und vielen Daten) sollten Sie die Indexe möglicherweise 
optimieren.<br>
Die Erneuerungs-Option stellt den Index wieder her. Dies ist lediglich eine
Sicherheitsoption auf die nur im Fall von Problemen zurückgegriffen werden sollte. Sie könnnen
die Indexe auch einzeln nach Typ erneuern und optimieren. Falls notwendig können
Sie auch alle Indexe auf einmal erneuern, indem Sie die Option &quot;Alle erneuern&quot; wählen. 
<hr>

<A NAME="online_state"></A>
<h2>Systemverfügbarkeit</h2>
In manchen Fällen möchten Sie vielleicht die Anmeldung von Mitgliedern verhinden,
z.B. um Instandhaltungsarbeiten durchzuführen oder etwas im Verwaltungsbereich zu
konfigurieren. Um zu vermeiden, dass Sie Cyclos ausschalten oder eine Fehlerseite
anzeigen müssen, können Sie mit Hilfe dieser Option verhindern, dass 
Mitglieder sich anmelden oder auch um bereits angemeldete Mitglieder abzumelden. 
Nur Administratoren mit der Berechtigung, das System wieder Online zu setzen, sind in der Lage,
das System abzuschalten (Offline setzen).
<br>
(Die Berechtigung sinden Sie unter: Administrator-Gruppenberechtigungen >  
Administrative Aufträge > System verfügbar setzen)
<hr>

<A NAME="import_export"></A>
<h2>Import/Export Einstellungen</h2>
Mit dieser Funktion können Sie Einstellungen exportieren und importieren, um sie mit anderen Cyclos-Instanzen 
teilen zu können. Alle Einstellungen außer den Einstellungen für Kommunikationswege und Web-Services können 
exportiert und importiert werden (diese Ausnahmen sind normalerweise für jede Instanz einmalig). 
Die Export-Funktion ist recht einfach. Der Export generiert eine (lesbare) XML-Datei. 
Der Import übernimmt die Einstellungen, die in einer XML-Datei definiert wurden. 
<hr>
 
 </div> <%--  page-break end --%>
 
<div class='help'>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
<p>&nbsp;</p>
</div>