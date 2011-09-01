<div style="page-break-after: always;">
<p class="head_description">Cyclos kategorisiert Benutzer nach Gruppen. Jeder Benutzer 
im System kann nur einer einzigen Gruppe angehören. Es gibt drei <a
	href="#group_categories"><u>Hauptkategorien</u></a> von Gruppen.<br>
Die Gruppen sind dazu da, Benutzern der Software die entsprechenden  
<a href="#permissions"><u>Berechtigungen</u></a> zu erteilen. Ein Benutzer hat 
keinen Zugang zu Cyclos-Funktionalitäten wenn seine Gruppe keine Berechtigung dazu erteilt 
hat. Natürlich ist es möglich, die Gruppenberechtigungen zu verändern, oder Benutzer aus 
einer Gruppe in die andere zu verschieben.<br>

Zusätzlich zu den Berechtigungen kann eine Gruppe außerdem spezifische  
<a href="#edit_member_group"><u>Einstellungen</u></a> haben, die das Verhalten dieser 
Gruppe bestimmen, wie z.B. Limits oder aber den erlaubten Zugang. Mitgliedsgruppen 
haben mehr Konfigurationsoptionen als Administratorengruppen. In den 
Mitglieds-Gruppeneinstellungen können Sie z.B. definieren, welche Konten diese Gruppe hat, 
ebenso wie Layout und Anzeige-Elemente für diese spezifische Gruppe. 
<p>
Die Tatsache, dass ein Mitglied nur in einer Gruppe sein kann, bedeutet allerdings nicht, 
dass alle Systemkonfigurationen gruppenspezifisch sind. Das System ist sehr flexibel. 
Konfigurationen und Einstellungen existieren auf der Systemebene, auf der Gruppenebene und 
auf der individuellen Ebene. Wenn die gleiche Einstellung auf unterschiedlichen Ebenen 
existiert, so hat die untere Ebene stets Priorität. Ein individuelles Kreditlimit überschreibt 
z.B. ein Gruppenkreditlimit. Und eine benutzerdefinierte Gruppenseite (zum Beispiel die Kontaktseite) 
überschreibt die Systemkontaktseite.<br>
Viele Konfigurationen können für mehrere Gruppen eingestellt werden. Zum Beispiel kann ein 
Beitrag gleich für mehrere Mitgliedsgruppen erhoben werden. 
<p>
Grundsätzlich ist Cyclos für eine Reihe von Standardgruppen eingestellt. Normalerweise sind 
diese Gruppen für den Betrieb des Systems ausreichend.<br>
Die Standardgruppen können Sie nicht nur für die Erteilung der Berechtigungen, sondern auch 
für die Verwaltung der Gruppen verwenden. Wenn Sie zum Beispiel ein Mitglied aus dem System 
entfernen möchten, können Sie es ganz einfach in die Gruppe &quot;Gelöschte Mitglieder&quot; 
verschieben, über die Funktion &quot;Gruppe ändern&quot;. Diese Funktion zeichnet ebenfalls 
alle Gruppenveränderungen auf, zusammen mit dem Datum, der Uhrzeit und dem Namen des ausführenden 
Administrators.<br>
Obwohl die Konfiguration mit den Standardgruppen für die meisten Organisationen gut geeignet 
ist, ist es auch möglich, neue Gruppen einzurichten. Dies sollte allerdings nur dann getan werden, 
wenn bereits Erfahrungen mit dem Betrieb des Systems gesammelt wurde. 
<p>
<span class="admin"> <i>Wo ist es zu finden?</i><br>
Die Verwaltung der Gruppen finden Sie unter &quot;Menü: Benutzer und Gruppen > 
Gruppen&quot;.<br>
Gruppenfilter finden Sie unter &quot;Menü: Benutzer und Gruppen > Gruppenfilter&quot;. 
<p>
<i>Wie werden die Gruppen aktiviert?</i><br>
Ein neues Mitglied ist immer Teil einer Gruppe. Daher muss bei der Einrichtung eines neuen 
Mitglieds oder Administrators eine Gruppe gewählt werden. Am besten tun Sie dies im Abschnitt  
&quot;Benutzer und Gruppen > <a href="${pagePrefix}user_management#search_member_by_admin"><u>
Mitglieder verwalten</u></a>&quot; und &quot;Benutzer und Gruppen > <a
	href="${pagePrefix}user_management#search_admin"><u>Administratoren verwalten</u></a>&quot;.<br>
</span></p>
<hr>

<a name="group_categories"></a>
<p class="sub_description">
<h2>Hauptkategorien für Gruppen</h2>
Es gibt drei Hauptkategorien von Gruppen: 
<ul>
	<li><a href="#member_groups"><u>Mitgliedsgruppen</u></a> - normale Mitglieder 
	mit Zugang zum Mitgliedsbereich von Cyclos. 
	<li><a href="#broker_groups"><u>Brokergruppen</u></a> - eine Art 
	&quot;Super-Mitglieder&quot;, also Mitglieder mit Zugang zu gewissen Verwaltungsfunktionen 
	für	eine Reihe von anderen Mitgliedern.
	<li><a href="#admin_groups"><u>Administratorengruppen</u></a> - Benutzer mit 
	Verwaltungsfunktionen. 
</ul>
Diese Unterteilung in Hauptkategorien geschieht aus Sicherheitsgründen, so dass es 
unmöglich ist, einem Mitglied aus Versehen Administratoren-<a href="#permissions"><u>Berechtigungen</u></a>
zu erteilen.<br>
Alle diese Gruppen sind mit als Standard eingestellten Berechtigungen ausgestattet, 
allerdings können diese auch verändert werden. 
</p>
<hr>

<a name="member_groups"></a>
<p class="sub_description">
<h2>Standard Mitgliedsgruppen</h2>
Mitglieder dieser Gruppen haben Zugang zum Mitgliedsbereich von Cyclos. Das System ist 
mit den folgenden Standard-Mitgliedsgruppen ausgestattet: 
<ul>
	<li><a href="#inactive_members"><u>Inaktive Mitglieder</u></a>:&nbsp;Ist diese Gruppe als 
	Startgruppe eingestellt, können Benutzer sich nicht anmelden, sondern müssen um sich 
	anmelden zu können zuvor noch von eine Administrator  &quot;aktiviert&quot; (d.h. in 
	eine aktive Mitgliedsgruppe verschoben) werden.  
	<li><a href="#full_members"><u>Vollwertige Mitglieder</u></a>:&nbsp;Ganz normale Mitglieder.
	<li><a href="#disabled_members"><u>Gesperrte Mitglieder</u></a>:&nbsp;Zweitweise inaktive Mitglieder.
	<li><a href="#removed_members"><u>Gelöschte Mitglieder</u></a>:&nbsp; Mitglieder die 
	definitiv das System verlassen haben. 
</ul>
</p>
Die Standardeinstellungen sind nicht unveränderlich, sondern wurden unter Verwendung der 
Gruppen-<a href="#permissions"><u>Berechtigungen</u></a> und <a href="#edit_member_group">
<u>Einstellungen</u></a> eingerichtet, von denen wir annehmen dass Sie am Häufigsten verwendet 
werden. Es ist also möglich, diese Gruppen zu verändern, oder neue Gruppen mit unterschiedlichen 
Konfigurationen einzurichten. 
<hr class="help">

<a name="inactive_members"></a>
<h3>Inaktive Mitgliedergruppe</h3>
Wenn sich ein Benutzer über die Registrierungsseite registriert, wird er automatisch 
in die Gruppe &quot;Inaktive Mitglieder&quot; verschoben.  Mitglieder dieser Gruppe 
können sich nicht anmelden, haben keinen Zugang zum System und kein aktives Konto. 
Kontoadministratoren können eine Liste der Mitglieder dieser Gruppe anfordern; nach 
einer Überprüfung kann das Mitglied in die &quot;aktive&quot; Gruppe verschoben werden. 
Normalerweise ist dies die Gruppe <a href="#full_members"><u>Vollwertige Mitglieder</u></a>. 
<br>
Da diesen inaktiven Mitglieder keine Konten zugeordnet sind, können diese Mitglieder auch 
vollständig aus dem System entfernt werden. Sobald ein Mitglied Teil einer &quot;aktiven&quot; 
Gruppe ist (mit Konto), kann das Mitglied nicht mehr gelöscht werden, sondern kann nur in die 
Gruppe <a href="#removed_members"><u>Gelöschte Mitglieder</u></a> verschoben werden. Mehr 
Informationen zum inaktiven/aktiven Status der Gruppe finden Sie im Abschnitt 
<a href="#group_details"><u>Gruppen-Details</u></a>. 

<hr class="help">

<a name="full_members"></a>
<h3>Vollwertige Mitgliedergruppe</h3>
Dies ist die normale Gruppe für Mitglieder. Ein &quot;Vollwertiges Mitglied&quot; kann sich 
anmelden, und jede Mitgliedsfunktionalität wählen. Wird eine Mitglied (Interessent) aus der 
Gruppe &quot;Inaktive Mitglieder&quot; in die Gruppe der &quot;Vollwertigen Mitglieder&quot; 
verschoben, so ist das Mitglied aktiviert, und kann ein Konto mit dem als Standard eingestellten 
Startguthaben (falls konfiguriert) und ein Kennwort zum Anmelden in das System erhalten.<br> 
Falls konfiguriert kann das Mitglied eine &quot;Aktivierungsmail&quot; mit Informationen zum 
Anmelde- und Kontostatus erhalten. 
<br>
<hr class="help">

<a name="disabled_members"></a>
<h3>Gesperrte Mitgliedergruppe</h3>
Hat ein Administrator ein Mitglied in die Gruppe &quot;Gesperrte Mitglieder&quot; verschoben, 
kann das Mitglied sich nicht mehr anmelden. Das Konto ist dann im &quot;Ruhezustand&quot;. Von 
Mitgliedern dieser Gruppe werden keine Gebühren und Beiträge erhoben. Die einzige aktive Funktion 
eines &quot;Gesperrten Mitglieds&quot; ist, dass es immer noch Zahlungen erhalten kann (sich aber 
nicht anmelden kann um dies zu überprüfen).<br>
Inserate eines gesperrten Mitglieds erscheinen nicht in den von anderen Mitgliedern getätigten 
Inseratssuchen.  Allerdings erscheint das Profil des gesperrten Mitglieds in der Mitgliedersuche. 
Beim Lesen des Profils eines gesperrten Mitglieds informiert eine Nachricht darüber, dass dieses 
Mitglied gesperrt ist, und (zur Zeit) keinen Zugang zum System hat.<br> 
Um wieder aktiviert zu werden, muss ein Mitglied dieser Gruppe von einem Administrator zurück in die 
Gruppe <a href="#full_members"><u>Vollwertige Mitglieder</u></a> verschoben werden.<br>
Ein typischer Grund, eine Mitglied zu &quot;sperren&quot; ist dessen Abwesenheit während eines 
längeren Zeitraums (wie z.B. für vier Monate). Dieser Status kann auch für verdächtige oder fragwürdige 
Mitglieder verwendet werden, um deren Anmeldung zu verhindern (und weitere Informationen ein 
zu holen). 

<hr class="help">

<a name="removed_members"></a>
<h3>Gelöschte Mitgliedergruppe</h3>
Der Grund, ein Mitglied in die Gruppe &quot;Gelöschte Mitglieder&quot; zu verschieben, ist, dass das 
Mitglied das System verlassen hat. Wurde ein Mitglied erst in diese Gruppe verschoben, kann es keiner 
anderen Gruppe mehr zugeteilt werden. Die Daten werden in der Datenbank gespeichert, und können von 
Administratoren eingesehen werden, haben allerdings nur noch Backup-Funktion.<br>
Alle Daten (Inserate, Profil) eines gelöschten Mitglieds sind für andere Mitglieder nicht mehr sichtbar. 
Lediglich der Überweisungsverlauf zeigt noch die zurückliegenden Überweisungen in Zusammenhang mit diesem 
Mitglied. Wählt ein aktives Mitglied den Mitgliedsnamen (des gelöschten Mitglieds) von einem alten 
Überweisungsverlauf, so erhält es statt des Profils eine Nachricht, dass dieses Mitglied gelöscht wurde. 
Mitglieder, deren Kontaktliste noch ein gelöschtes Mitglied enthält, erhalten die gleiche Meldung. 
<p>
Die &quot;gelöschte&quot; Gruppe hat lediglich Archivfunktion. Wird nach einigen Jahren das System 
bereinigt, weiß der durchführende Administrator, dass diese Daten ohne Risiko (gesichert) gelöscht 
werden können.<br>
<b>Anmerkung:</b> Von dieser Regel gibt es eine Ausnahme. Ein Mitglied, das niemals zu einer Gruppe 
mit Konten gehört hat, kann definitiv aus dem System entfernt werden. Dafür gibt es eine separate 
Administrations-Berechtigung.<br>
<hr>

<a name="broker_groups"></a>
<p class="sub_description">
<h2>Standard Brokergruppen</h2>
Broker sind eine Art &quot;Super-Mitglieder&quot;, also Mitglieder mit Zugang zu gewissen 
Verwaltungsfunktionen für eine Reihe von anderen Mitgliedern. Sie haben Zugang zum Mitgliedsbereich 
von Cyclos.<br> 
Die folgenden Standard-Brokergruppen sind in Cyclos verfügbar: 
<ul>
	<li><a href="#full_brokers"><u>Vollwertige Broker</u></a>: der normale, Standard-Brokertyp. 
	<li><a href="#disabled_brokers"><u>Gesperrte Broker</u></a>: um einen Broker zeitweise zu sperren. 
	<li><a href="#removed_brokers"><u>Gelöschte Broker</u></a>: für dauerhaft gelöschte Broker. 
</ul>
</p>
So wie die Standardeinstellungen für Mitglieder, sind auch diese nicht unveränderlich, sondern wurden 
unter Verwendung der Gruppen-<a href="#permissions"><u>Berechtigungen</u></a> und 
<a href="#edit_broker_group"><u>Einstellungen</u></a> eingerichtet, von denen wir annehmen dass Sie 
am Häufigsten verwendet werden. Es ist also möglich, diese Gruppen zu verändern, oder neue Gruppen 
mit unterschiedlichen Konfigurationen einzurichten. 

<hr class="help">

<a name="full_brokers"></a>
<h3>Vollwertige Broker</h3>
Ein Broker ist ein Mitglied mit zusätzlichen Funktionen. Ein Broker kann andere Mitglieder registrieren, 
und hat, je nach Systemkonfiguration, in gewissem maße Zugang zu den Mitgliedern, deren Broker er ist. 
Registriert ein Broker ein Mitglied, so muss dieses Mitglied zunächst durch einen Administrator 
aktiviert werden. Es ist ebenfalls möglich, dass ein Broker Mitglieder direkt in eine oder mehrere 
&quot;aktive&quot; Gruppen verschieben kann, dies muss allerdings so konfiguriert sein. Ebenso ist es 
möglich, dass ein Broker für die (Überweisungs-)Aktivitäten seiner Mitglieder eine 
<a href="${pagePrefix}brokering#commission"><u>Kommission</u></a> erhält. Eine Kommission ist für diese 
Brokergruppe konfiguriert, aber noch nicht aktiviert. 

<hr class="help">

<a name="disabled_brokers"></a>
<h3>Gesperrte Brokergrupppe</h3>
Ein Broker in dieser Gruppe kann sich nicht anmelden, auch nicht als Mitglied. Das Konto ist noch aktiv, 
d.h. er kann Zahlungen erhalten und es werden ihm Gebühren berechnet (falls zutreffend).<br>
Diese Gruppe können Sie verwenden, um einen Broker zeitweise zu sperren, z.B. weil er für eine längere 
Zeit auf Reisen ist, oder wenn sich nachfragen ergeben (falls Sie z.B. einen Missbrauch oder Betrug 
durch den Broker vermuten). 
<p>
Für weitere Erklärungen zu den Brokern, wenden Sie sich bitte an den Hilfebereich für
<a href="${pagePrefix}brokering"><u>Broker</u></a>. 

<hr class="help">

<a name="removed_brokers"></a>
<h3>Gelöschte Brokergruppe</h3>
Dieser Gruppe ähnelt in vielerlei Hinsicht der Gruppe 
<a href="#removed_members"><u>&quot;Gelöschte Mitglieder&quot;</u></a>. Wurde ein Broker in die Gruppe 
&quot;Gelöschte Broker&quot; verschoben, so ist der Verlauf &quot;Broker-Mitglieder&quot; für 
Administratoren immer noch sichtbar.<br> 
Achtung: Dieser Broker kann niemals wieder in eine andere Gruppe verschoben werden; 
&quot;Gelöscht&quot; bedeutet wirklich &quot;Gelöscht&quot;. 
<p>
Für weitere Erklärungen zu den Brokern, wenden Sie sich bitte an den Hilfebereich für
<a href="${pagePrefix}brokering"><u>Broker</u></a>.

<hr>
<a name="admin_groups"></a>
<p class="sub_description">
<h2>Standard Administratorengruppen</h2>
Benutzer in der Administratorengruppe können innerhalb der Software administrative Handlungen 
vornehmen. Sie haben Zugang zum Administratorenbereich von Cyclos. 
<span class="admin">
Grundsätzlich ist Cyclos für die folgenden Standard-Administratorengruppen eingestellt:  
<ul>
	<li><a href="#system_admins"><u>Systemadministratoren</u></a>: können alle Administrationsfunktionen wahrnehmen.
	<li><a href="#account_admins"><u>Kontenadministratoren</u></a>: für die Verwaltung in Zusammenhang mit den Mitgliedern. 
	<li><a href="#disabled_admins"><u>Gesperrte Administratoren</u></a>: zeitweilig inaktive Administratoren. 
	<li><a href="#removed_admins"><u>Gelöschte Administratoren</u></a>: dauerhaft entfernte Administratoren. 
</ul>
</p>
Alle diese Gruppen sind mit als Standard eingestellten <a href="#permissions"><u>Berechtigungen</u></a> 
ausgestattet, allerdings können diese auch verändert werden. 
</span>
<hr class="help">

<span class="admin">
<a name="system_admins"></a>
<h3>Systemadministratoren</h3>
Dieser Gruppe angehörende Benutzer können alle Administrationsfunktionen verwenden, einschließlich 
der Einrichtung neuer Administratoren, der Erteilung der Berechtigungen und der Änderung der 
Systemkonfigurationen. Es empfiehlt sich, die Gruppe der Systemadministratoren nur für Konfigurationen 
heran zu ziehen, und nicht für operative Aufgaben. 

<hr class="help">
</span>

<span class="admin">
<a name="account_admins"></a>
<h3>Kontenadministratoren</h3>
Zu dieser Gruppe gehörende Benutzer können alle Verwaltungsaufgaben in Zusammenhang mit Mitgliedern 
und mit Inseraten ausführen. Ein Kontoadministrator kann keine Systemeinstellung bzw. Konfiguration 
verändern. Ebenso hat ein Kontoadministrator Zugang zu allen Anzeige-Funktionen, wie Systemstatus, 
Statistiken, etc. Es ist möglich, Gruppen von Kontoadministratoren für die Verwaltung bestimmter 
Mitgliedsgruppen einzurichten, um die Kontoadministration zu teilen. 
<hr class="help">
</span>

<span class="admin">
<a name="disabled_admins"></a>
<h3>Gesperrte Administratoren</h3>
Zu dieser Gruppe gehörende Administratoren können rein gar nichts tun, nicht einmal sich anmelden. 
Diese Gruppe kann für zeitweilig gesperrte Administratoren verwendet, so dass sie nicht gleich ganz 
gelöscht werden müssen. 
<hr class="help">
</span>

<span class="admin">
<a name="removed_admins"></a>
<h3>Gelöschte Administratoren</h3>
Dies ist die Gruppe für definitiv aus dem System entfernte Administratoren. Bitte bedenken Sie, 
dass es wie  bei den <a href="#removed_members"><u>Gelöschte Mitglieder</u></a>, hier keinen Weg 
zurück gibt. Sind sie erst einmal gelöscht, können Administratoren nicht &quot;zurück geholt&quot; 
werden. Die einzige noch offene Option ist, sie ganz aus dem System und der Datenbank zu entfernen. 

<hr class="help">
</span>

<span class="admin">
<a name="change_group"></a>
<h3>Gruppe ändern</h3>
Hier können Sie die Gruppe ändern, der das Mitglied (oder der
<a href="${pagePrefix}brokering"><u>Broker</u></a>) angehört. Wählen Sie die neue 
Gruppe einfach im Auswahlfeld. Zu dieser Veränderung müssen Sie allerdings im Textfeld 
&quot;Beschreibung&quot; einen Kommentar abgeben. Speichern Sie diese Änderungen durch 
Anklicken der Schaltfläche &quot;Weiter&quot;. 
<p>
Für eine Übersicht der  <a href="#member_groups"><u>Mitgliedsgruppen</u></a> bitte hier
klicken.
<p>
Nachdem Sie die Gruppen-Änderung eingegeben haben, wird sie im Verlaufsfeld gespeichert, 
in chronologischer Anordnung, mit der jeweils neuesten Zugehörigkeit ganz oben in der Liste. 
Der Kommentarverlauf zeigt über jedem Kommentar eine Statuszeile, mit dem Namen des 
Administrators, der die Änderung eingegeben hat, dem Datum und der vorgenommenen 
Veränderung (&quot;von Gruppe X in Gruppe Y&quot;).<br>
Auf diese Weise haben die Administratoren eine schnelle Übersicht darüber, was mit dem 
Mitgliedskonto geschehen ist, und können nachlesen, warum sich was verändert hat. Als Kommentar 
reicht ein kurzer Satz mit dem Änderungsgrund. Jede weitere Information  sollte 
in der Funktion <a href="${pagePrefix}profiles#member_info_actions"><u>Bemerkungen</u></a> 
eingegeben werden.
<p>
Einige Bemerkungen zur Änderung von Gruppen: 
<ul>
	<li>Ist ein Mitglied in der Gruppe <a href="#inactive_members"><u>Inaktive Mitglieder</u></a> 
	haben Sie die Option, das Mitglied ganz aus dem System zu löschen. Dies ist hilfreich bei 
	doppelten oder unseriösen Registrierungen. Ist ein Mitgliedskonto erste einmal aktiviert, 
	kann es nicht mehr einfach gelöscht werden. Sie können es allerdings in &quot;Gelöschte 
	Mitglieder&quot; verschieben. 
	<li>Wenn Sie ein Mitglied aus einer <a href="#full_brokers"><u>Broker</u></a>-Gruppe
	in eine normale (Nicht-Broker) Mitgliedsgruppe verschieben, dann haben alle Mitglieder, 
	die unter dem Broker zu finden waren, keinen Broker mehr. (Dies ist nicht der Fall, wenn 
	Sie den Broker in eine andere Broker-Gruppe verschieben, z.B. in <a href="#disabled_brokers"><u>
	Gesperrte Broker</u></a>). Falls Sie die Mitglieder nicht ohne Broker lassen wollen, zuerst 
	den Broker für alle betroffenen Mitglieder zu ändern, und erst dann den ursprünglichen 
	Broker in eine neue Gruppe zu verschieben. Dies kann über die  
  <a href="${pagePrefix}user_management#bulk_actions"><u>Massenaktionen</u></a> geschehen.
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="change_group_admin"></a>
<h3>Administratorengruppe ändern</h3>
Hier können Sie die 
<a href="#admin_groups"><u>Administratorengruppe</u></a> ändern, der ein Administrator 
angehört. Wählen Sie die neue Gruppe einfach im Auswahlfeld. Zu dieser Veränderung 
können Sie im Textfeld &quot;Beschreibung&quot; einen Kommentar abgeben (z.B: Warum 
die Gruppe geändert wird). Denken Sie bitte daran, die Weiter-Schaltfläche &quot;Gruppe 
ändern&quot; anzuklicken, nachdem Sie Ihren Text eingegeben haben. 
<p>
Nachdem Sie die Gruppen-Änderung eingegeben haben, wird sie im Verlaufsfeld gespeichert, 
in chronologischer Anordnung, mit der jeweils neuesten Zugehörigkeit ganz oben in der 
Liste. Der Kommentarverlauf zeigt über jedem Kommentar eine Statuszeile, mit dem Namen 
des Administrators, der die Änderung eingegeben hat, dem Datum und der vorgenommenen 
Veränderung (&quot;von Gruppe X in Gruppe Y&quot;). 
<p>
Auf diese Weise haben die Administratoren eine schnelle Übersicht darüber, was mit dem 
Administratorenkonto geschehen ist, und können nachlesen, warum sich was verändert hat. 
Als Kommentar reicht ein kurzer Satz mit dem Änderungsgrund. Jede weitere Information zu 
einem Administrator sollte in den Bemerkungen eingegeben werden. 
<p>
In diesem Fenster ist es auch möglich, einen Administrator ganz aus dem System zu entfernen, 
sollte dies notwendig sein. Der bevorzugte Weg sollte allerdings sein, ihn in die Gruppe 
&quot;Gesperrte Administratoren&quot; zu verschieben. 

<hr class="help">
</span>

<span class="admin">
<a name="group_management"></a>
<p class="sub_description">
<h2>Gruppenverwaltung</h2>
In Cyclos können Sie eine Reihe von Guppen-Aktionen durchführen. Ändern können Sie 
Gruppeneigenschaften und Berechtigungen; und Sie können Berechtigungsgruppen löschen. 
</p>
Die Gruppenverwaltung finden Sie über &quot;Menü: Benutzer und Gruppen > Gruppen&quot;.
<hr>
</span>

<span class="admin">
<a name="search_groups"></a>
<h3>Gruppen suchen</h3>
Suchen können Sie über eine der 
<a href="#group_categories"><u>Kategorien</u></a> im &quot;Typen-Auswahlfeld&quot;. 
Hat das System <a href="#group_filters"><u>Gruppenfilter</u></a> , so erscheint ebenfalls 
eine Suchoption für diese Filter. 
<hr class="help">
</span>

<span class="admin">
<a name="manage_groups"></a>
<h3>Gruppen verwalten</h3>
In diesem Fenster können Sie die verschiedenen Gruppen verwalten. Das Fenster zeigt eine 
Übersicht der verfügbaren Gruppen, und bietet die Möglichkeit, neue Gruppen einzurichten. 
<p>
Klicken Sie für eine der aufgelisteten Gruppen auf eines der folgenden Symbole: 
<ul>
	<li><img border="0" src="${images}/edit.gif" width="16" height="16">&nbsp;Auswahl 
	des Bearbeiten-Symbols öffnet ein Fenster, in dem Sie die Gruppen-Eigenschaften einstellen 
	können. 
	<li><img border="0" src="${images}/permissions.gif" width="16" height="16">&nbsp;Das 
	Berechtigungs-Symbol führt Sie zu einer Seite, auf der Sie die Berechtigungen für diese 
	Gruppe einstellen können. 
	<li><img border="0" src="${images}/permissions_gray.gif" width="16" height="16">&nbsp;
	Ist das Berechtigungs-Symbol grau, bedeutet das, dass für diese Gruppe keine Berechtigungen 
	eingestellt werden können, da es sich um eine Gruppe für "Gelöschte Mitglieder" handelt. 
	Mitglieder dieser Gruppe werden aus dem System entfernt, allerdings bleibt ein Teil ihrer 
	Daten (z.B. Überweisungen) im System erhalten. Für mehr Information dazu, wenden Sie sich 
	bitte an die <a	href="#insert_group"><u>Hilfe</u></a> für &quot;neue Gruppe&quot;.
	<li><img border="0" src="${images}/delete.gif" width="16" height="16">&nbsp;
	Durch Auswahl des Löschen-Symbols können Sie die Gruppe entfernen. Gruppen können nur dann 
	entfernt werden, wenn sich darin kein Benutzer befindet. 
</ul>
Um eine neue Gruppe einzufügen, klicken Sie bitte auf die Schaltfläche &quot;Neue 
Gruppe einfügen&quot;. Wir empfehlen eindeutig, dies erst dann zu tun, wenn Sie bereits 
Erfahrung mit der Bearbeitung der Standard-Gruppen gesammelt haben. 
<br>
Für eine Übersicht der <a href="#group_categories"><u>Gruppenkategorien</u></a> bitte
hier klicken.
<hr class="help">
</span>

<span class="admin">
<a name="edit_admin_group"></a>
<h3>Administratorengruppe ändern</h3>
Hier können Sie die Eigenschaften einer <a href="#admin_groups"><u>Administratorengruppe</u></a>
bearbeiten bzw. ändern. Nach Anklicken der Schaltfläche &quot;Bearbeiten&quot;, können Sie 
den Namen, die Beschreibung und die Zugangseinstellungen ändern. Der Status 
kann nur bei der Einrichtung der Gruppe definiert werden. 
<p>
Bitte beachten Sie, dass Sie hier keine Berechtigungen einstellen können. Gruppeneigenschaften 
und Einstellungen sind nicht das Gleiche wie Berechtigungen. Durch Anklicken des 
Berechtigungs-Symbols <img border="0" src="${images}/permissions.gif" width="16"
	height="16"> in den <a href="#manage_groups"><u>Gruppeneinstellungen</u></a>, 
können Sie die Gruppenberechtigungen ändern, Sie können allerdings auch die Schaltfläche 
mit der Beschriftung &quot;Gruppenberechtigungen&quot; unter dem Fenster benutzen. 
<p>
Die folgenden Gruppeneinstellungen sind verfügbar:
<ul>
	<li><b>Kennwortlänge:</b> Minimale und maximale Länge des Kennworts.<br>
	<li><b>Kennwortüberwachung:</b> Hier können Sie sich für eine von drei Optionen 
	entscheiden: (Kein Kennwort erzwingen, erkennbare Kennwörter wie Geburtsdatum verbieten, 
	nur Buchstaben und Zahlen zulassen). Wird eine Kennwortüberwachung gewählt, können die Benutzer 
	als neues Kennwort kein in der Vergangenheit benutztes wählen. 
	<li><b>Maximum Kennwortversuche:</b> Erreicht der Benutzer die maximale Anzahl der Versuche, 
	kann der Benutzer sich bis zum Ablauf der Sperrzeit nicht wieder anmelden (siehe nächste 
	Einstellung).<br>
	<li><b>Sperrzeit nach maximalen Kennwortversuchen:</b> Dies ist der Zeitraum, während dessen 
	ein Benutzer sich nach Erreichen der maximalen Anzahl an Kennwortversuchen nicht mehr 
	anmelden kann.<br>
	<li><b>Benutzerkennwort abgelaufen nach:</b> Mit dieser Einstellung können Sie den Zeitraum 
	definieren, während dessen das Benutzerkennwort gültig ist. Läuft der Zeitraum ab, so muss 
	das Mitglied ein neues Passwort eingeben. Wenn Sie hier &quot;0&quot; eingeben, läuft das 
	Kennwort niemals ab.<br>
	<li><b>Überweisungskennwort:</b> Hier können Sie die Verwendung eines speziellen Kennworts für 
	Überweisungen einstellen. Sie haben die folgenden Optionen: 
	<ul>
		<li><b>&quot;Nicht verwendet&quot;</b>: Es werden keine Überweisungskennwörter verwendet, 
		und Mitglieder können jede Überweisung durchführen (falls sie die entsprechenden 
		Berechtigungen haben), ohne zuerst ein Überweisungskennwort einzugeben. 
		<li><b>&quot;Automatisch&quot;</b>: Wird die Option &quot;Automatisch&quot; gewählt, 
		generiert das System bei Einrichtung des Kontos eines neuen Mitglieds automatisch ein 
		Überweisungskennwort (oder ab jetzt, für bereits existierende Mitglieder). Das Mitglied 
		erhält das Kennwort (nur einmal) in seinem persönlichen Nachrichtenbereich. 
		<li><b>&quot;Manuell&quot;</b>: Wird dies gewählt, kann das Überweisungskennwort nur 
		manuell generiert werden, über <a href="${pagePrefix}profiles#access_actions">
		<u>&quot;Überweisungskennwort verwalten&quot;</u></a> im Profil unter Mitglieds-Aktionen.
	</ul>
	<li><b>Überweisungskennwortlänge:</b> Einstellen der Länge des Überweisungskennworts. 
	Dieses Kennwort hat stets eine festgelegte Länge. (Diese Einstellung hat natürlich 
	keinerlei Wirkung, wenn kein Überweisungskennwort verwendet wird). 
	<li><b>Maximale Versuche Überweisungskennwort:</b> Nach dieser Anzahl fehlgeschlagener 
	Versuche wird das Kennwort gesperrt. Ein Administrator kann das Kennwort über 
	<a href="${pagePrefix}profiles#access_actions"><u>&quot;Überweisungskennwort verwalten&quot;</u></a>  
	zurücksetzen. (Diese Einstellung hat natürlich keinerlei Wirkung wenn kein 
	Überweisungskennwort verwendet wird). 
	</li>
</ul>
Denken Sie bitte daran, die Schaltfläche &quot;Weiter&quot; anzuklicken, nachdem Sie 
Ihre Änderungen eingegeben haben. 

<hr class="help">
</span>

<span class="admin">
<a name="edit_member_group"></a>
<h3>Mitgliedsgruppen ändern</h3>
Hier können Sie die Eigenschaften einer 
<a href="#member_groups"><u>Mitgliedsgruppep</u></a> bearbeiten bzw. ändern.
Nach Anklicken der Schaltfläche &quot;Bearbeiten&quot;, können Sie den Namen, die Beschreibung 
und einige andere Einstellungskategorien ändern. 
<p>
Bitte beachten Sie, dass Sie hier keine Berechtigungen einstellen können. 
Gruppeneigenschaften und Einstellungen sind nicht das Gleiche wie Berechtigungen. 
Durch Anklicken des Berechtigungs-Symbols <img border="0" src="${images}/permissions.gif" width="16"
	height="16"> in der <a href="#manage_groups"><u>Gruppeneinstellungen</u></a>, können Sie 
die Gruppenberechtigungen ändern, Sie können allerdings auch die Schaltfläche mit der Beschriftung 
&quot;Gruppenberechtigungen&quot; unter dem Fenster benutzen. 
<p>
Einstellungen der Mitgliedsgruppen sind nach Kategorien geordnet. Die folgenden Kategorien 
sind verfügbar; klicken Sie auf die Links, um mehr Details zu den Feldern dieser 
Kategorien zu erhalten. 
<ul>
	<li><b><a href="#group_details"><u>Gruppendetails</u></a></b> sind grundlegende Eigenschaften. 
	<li><b><a href="#group_registration_settings"><u>Registrierungseinstellungen</u></a></b> 
	sind Einstellungen, die das Verhalten der Gruppe in Zusammenhang mit Mitgliedsregistrierung 
	definieren. Hier sind auch verschiedene andere Einstellungen beinhaltet. 
	<li><b><a href="#group_access_settings"><u>Zugangseinstellungen</u></a></b> sind 
	Einstellungen, die den Zugang der Gruppe definieren.
	<li><b><a href="#group_notification_settings"><u>Benachrichtigungseinstellungen</u></a></b> 
	behandeln die Benachrichtigungen an diese Gruppe.
	<li><b><a href="#group_ad_settings"><u>Inserate-Einstellungen</u></a></b>
	sind Einstellungen, die das Verhalten der Gruppe in Zusammenhang mit Inseraten definieren. 
	<li><b><a href="#group_scheduled_payment_settings"><u>Einstellungen geplante Zahlungen</u></a></b> 
	sind Einstellungen zu den 
	<a href="${pagePrefix}payments#scheduled"><u>geplanten Zahlungen</u></a>
	dieser Gruppe.
	<li><b><a href="#group_loans_settings"><u>Darlehenseinstellungen</u></a></b> sind 
	Einstellungen, die das Verhalten der Gruppe in Zusammenhang mit Darlehen definieren.
</ul>
<hr class="help">
</span>

<span class="admin"> <a name="group_details"></a>
<h3>Gruppendetails</h3>
Hier können Sie einige allgemeine Gruppeneinstellungen einstellen. Die folgenden 
Einstellungen sind verfügbar:  
<ul>
	<li><b>Typ:</b> Der Typ der Gruppe (Mitglied, Broker, Administrator). Dies wird bei der 
	<a href="#insert_group"><u>Gruppenerstellung</u></a> definiert, und kann nicht mehr 
	verändert werden.
	<li><b>Entfernt:</b> Dieses Feld zeigt, ob es sich um eine Gruppe handelt die 
	gelöschte Mitglieder enthält. Auch dies wird bei der <a href="#insert_group">
	<u>Gruppenerstellung</u></a> definiert und kann nicht mehr verändert werden.
	<li><b>Name:</b> Der Name der Gruppe. Auch dieser wird beim Erstellen der Gruppe
	definiert, kann aber verändert werden.
	<li><b>Name der Anmeldeseite:</b> Diese Option erscheint nur, wenn Sie die Anmeldeseite 
	für diese Gruppe definiert haben (im Fenster &quot;Benutzerdefinierte Dateien&quot;, unten). 
	Zugang zur benutzerdefinierten (Gruppen-)Anmeldeseite erhalten Sie, indem Sie den Namen 
	der Anmeldeseite nach der &quot;globalen&quot; Anmeldeseite, gefolgt von Fragezeichen, 
	eingeben. Der Name der Anmeldeseite kann keine Leerstellen  enthalten. 
	Hier ein Beispiel:<br>
	http://www.yourdomain.org/cyclos?yourgrouploginpagename.<br>
	Bitte beachten Sie, dass es auch möglich ist, den Namen der Anmeldeseite nach <a
		href="${pagePrefix}groups#group_filter"><u>Gruppenfilter</u></a> zu spezifizieren.
	<li><b>Containerseite URL:</b> Diese Einstellung wird verwendet, wenn Sie über eine 
	Website Zugriff auf Cyclos nehmen möchten. Die Einstellung funktioniert genau wie die 
	globale Containerseite  (siehe <a href="${pagePrefix}settings#local"><u>Einstellungen >
	Basiseinstellungen</u></a> aber nur für diese Gruppe. In dieses Feld müssen Sie die Seite setzen, 
	die den iframe oder den Frame-Set öffnet, der Cyclos enthält. Zum Beispiel: 
	http://www.yourgroupdomain.org/cycloswrapper.php<br>
	Bitte beachten Sie, dass es auch möglich ist, eine Anmelde-Container-Seite per  <a
		href="${pagePrefix}groups#group_filter"><u>Gruppenfilter</u></a> zu spezifizieren.
	<li><b>Beschreibung:</b> Hier können Sie die Beschreibung der Gruppe eingeben. Das Feld dient 
	lediglich der zusätzlichen Information in den Gruppeneinstellungen, und wird nirgendwo 
	sonst in Cyclos verwendet. 
	<li><b>Gruppe aktivieren</b> Diese Option erscheint nur für Gruppen, die kein Konto haben. 
	Falls Mitglieder dieser Gruppe für andere nicht sichtbar sein sollen, sollten Sie dieses 
	Auswahlkästchen leer lassen.<br>
	In einigen Fällen möchten Sie, dass Mitglieder für andere Benutzer sichtbar sind, obwohl 
	sie kein Konto haben. In diesem Fall verwenden Sie diese Option. Hier kann es sich um 
	Broker handeln, die lediglich Verwaltungsaufgaben erfüllen (und daher nicht selbst handeln), 
	oder aber um Demo-Benutzer, die einfach nur mal schauen möchten, was das System im Angebot hat, 
	und die auch nicht tauschen dürfen.<br>
	Wenn Sie diese Einstellung verändern, hat sie Effekt auf alle existierenden Mitglieder und neuen 
	Mitglieder der Gruppe. 
	<p>
</ul>
<hr class="help">
</span>

<span class="admin"> <a name="group_registration_settings"></a>
<h3>Registrierungseinstellungen für eine Gruppe</h3>
Dies sind Einstellungen, die das Gruppenverhalten in Zusammenhang mit der 
Registrierung definieren. Die Einstellungen sind Teil des Formulars 
&quot;<a href="#edit_member_group"><u>Mitgliedsgruppen ändern</u></a>&quot;. 
Die folgenden Einstellungen sind verfügbar: 
<ul>
	<li><b>Startgruppe</b>: Wenn Sie möchten, dass alle Benutzer direkt nach ihrer 
	Registrierung über die öffentliche Registrierungsseite erst einmal in einer Startgruppe 
	landen, können Sie dieses Kästchen anklicken. In Cyclos kann es eine oder mehrere 
	Startgruppen geben.<br>
	Sie können auch einen anderen Namen wählen, unter dem die Gruppe dann erscheint. 
	Dies erscheint nur, wenn es mehr als eine Startgruppe gibt. Der sich registrierende 
	Benutzer muss dann in einem Auswahlfeld einen der von Ihnen in diesem Feld definierten 
	Namen wählen.<br>
	<br>
	<li><b>E-Mail-Bestätigung</b>: Wird diese Option gewählt, ist eine E-Mail-Bestätigung 
	notwendig. Nach Eingabe und Anklicken von &quot;Weiter&quot; wird der Benutzer darüber 
	benachrichtigt,	dass er per E-Mail eine Bestätigung erhalten wird, mit der Aufforderung, 
	sie zu beantworten, damit die Registrierung weiter verarbeitet werden kann.<br>
	Solange bis die Registrierung bestätigt ist, und der maximale Registrierungszeitraum noch 
	nicht verstrichen ist, behält die Registrierung den Status &quot;Anstehend&quot;. Den maximalen 
	Registrierungszeitraum können Sie im Abschnitt &quot;Limits&quot; der Hilfe für <a
		href="${pagePrefix}settings#local"><u>Basiseinstellungen</u></a> einstellen.
	Die Mitglieder mit dem Status &quot;Anstehend&quot; können auf der Seite <a
		href="${pagePrefix}user_management#search_pending_member"><u>Interessenten</u></a> 
	angesehen werden.<br>
	Die folgenden Validierungs-Optionen sind verfügbar: 
	<ul>
		<li><b>Niemals:</b> Eine E-Mail-Bestätigung ist nicht erforderlich. Das Mitglied ist direkt Mitglied der 
		Startgruppe, die für die öffentliche Registrierung eingestellt ist. (oder, im Falle der 
		Registrierung durch Administrator oder Broker, Mitglied der von Administrator oder Broker 
		gewählten Gruppe). 
		<li><b>Nur bei der öffentlichen Registrierung:</b> Die Registrierung erfordert nur dann 
		eine E-Mail-Bestätigung, wenn Mitglieder sich über die öffentliche Registrierungsseite 
		registrieren. 
		<li><b>Bei der öffentlichen Brokerregistrierung:</b> Die Registrierung erfordert nur dann 
		eine E-Mail-Bestätigung, wenn Mitglieder sich über die öffentliche Registrierungsseite registrieren, 
		aber auch, wenn Broker ein Mitglied registrieren.
		<li><b>Bei allen Registrierungen (Administrator / Broker / öffentlich):</b> (erklärt sich von
		selbst)
	</ul>
	<br>
	<li><b>Nutzungsbedingungen</b> Hier wählen Sie die auf der Registrierungsseite 
	erscheinende Vereinbarung. Auf der Seite <a href="#list_registration_agreements">
	<u>Nutzungsbedingungen</u></a> können Sie neue Vereinbarungen hinzufügen.<br>
	Wird eine Nutzungsbedingung für eine Gruppe definiert, müssen alle Benutzer 
	dieser Gruppe diese Vereinbarung akzeptieren, um sich in Cyclos anmelden zu können. 
	Werden die Benutzer von einem Administrator oder Broker registriert, erscheint die 
	Vereinbarung bei ihrer ersten Anmeldung.<br>
	Werden bereits im System registrierte Benutzer in eine Gruppe mit definierter 
	Registrierunsgvereinbarung verschoben, erscheint auf dem Bildschirm des Benutzers beim 
	nächsten Anmeldeversuch diese Nutzungsbedingung (die er akzeptieren muss, bevor 
	er in der Lage ist, sich anzumelden).<br>
	Beim Wechsel zu einer neu erstellten Nutzungsbedingung, erscheint als zusätzliche 
	Option 
	&quot;Akzeptieren bei der nächsten Anmeldung erzwingen&quot;. Wird diese Option gewählt, 
	erscheint die Nutzungsbedingung für alle neuen, aber auch für die bereits 
	existierenden Benutzer dieser Gruppe. Wird sie nicht gewählt, müssen nur neu registrierte 
	Mitglieder sie akzeptieren.<br>
	<br>
	<li><b>Kennwort per E-Mail senden:</b> Wird diese Option gewählt, erhält der Benutzer nach 
	seiner Registrierung eine E-Mail mit dem Kennwort. Wird diese Option nicht gewählt, können 
	Benutzer (und Broker/Administratoren, je nach Berechtigungen) das Kennwort im 
	Registrierungsformular definieren.<br>
	<br>
	Die Nachricht können Sie unter <a href="${pagePrefix}settings#mail"><u>E-Mail Einstellungen</u></a> 
	definieren. Wenn Sie diese Option verwenden, stellen Sie bitte sicher, dass das 
	E-Mail-Feld ein Pflichtfeld ist. Dies kann in den <a href="${pagePrefix}settings#local"><u>
	Basiseinstellungen</u></a> eingestellt werden.<br>
	<br>
	<li><b>Maximale Anzahl Profil-Bilder je Mitglied:</b> Die maximale Anzahl der Bilder, die 
	ein Mitglied in sein <a href="${pagePrefix}profiles"><u>Profil</u></a> aufnehmen kann.<br>
	<br>
	<li><b>Mitgliedschaft endet nach:</b> Wenn Sie in diesem Feld einen anderen Wert als 
	&quot;0&quot; eingeben, erlischt die Mitgliedschaft in dieser Gruppe automatisch nach diesem 
	Zeitraum. Ist dieser Zeitraum nach Eintritt des Mitglieds in diese Gruppe verstrichen, wird 
	das Mitglied automatisch in eine andere Gruppe verschoben.<br>
	<br>
	<li><b>Gruppe nach Ablauf:</b> Haben Sie im vorherigen Punkt für die Mitgliedschaft eine Ablauffrist 
	eingestellt, so müssen Sie hier die Gruppe eingeben, in die das Mitglied nach Ablauf der 
	Frist verschoben wird. 
</ul>
<hr class="help">
</span>

<span class="admin"> <a name="list_registration_agreements"></a>
<h3>Nutzungsbedingungen</h3>
Dieses Fenster zeigt eine Liste von Nutzungsbedingungen. Weitere Information zu 
Nutzungsbedingungen finden Sie in der Hilfe <a href="#group_registration_agreement">
<u>Nutzungsbedingungen</u></a>.<br>
Hier können Sie eine bereits bestehende Vereinbarung verändern, oder eine neue einfügen.<br> 
Vereinbarungen können nur dann gelöscht werden, wenn keine Gruppe diese Vereinbarung verwendet, 
oder wenn kein Mitglied diese Vereinbarung akzeptiert hat.<br>
<br>
<hr class="help">
</span>

<span class="admin"> <a name="registration_agreement"></a>
<h3>Neue / Nutzungsbedingungen ändern</h3>
Eine Nutzungsbedingung ist ein Text, der auf der Registrierungsungsseite 
erscheinen kann. Benutzer, die sich registrieren wollen, MÜSSEN ein Kontrollkästchen 
anklicken, in dem sie bestätigen dass Sie die Vereinbarung akzeptieren, bevor 
die Schaltfläche &quot;Weiter&quot; angewählt werden kann.<br>
Vereinbarungen können an eine oder mehrere Gruppen gebunden sein. Dies kann in den  
<a	href="#group_registration_settings"><u>Einstellungen Registrierung</u></a> 
in den Gruppeneinstellungen definiert	 werden.<br>
Wenn Sie in einer Vereinbarung eine Änderung vornehmen, und möchten, dass auch die bereits 
existierenden Benutzer diese akzeptieren, müssen Sie eine neue Vereinbarung erstellen, 
und diese Veränderung in Gruppeneinstellungen vornehmen. (außerdem müssen Sie die Option 
&quot;Akzeptieren bei der nächsten Anmeldung erzwingen&quot; wählen). 

<hr class="help">
</span>


<span class="admin"> <a name="group_access_settings"></a>
<h3>Zugangseinstellungen der Gruppe</h3>
Zugangseinstellungen sind Einstellungen, die den Zugang der Gruppe definieren. Die 
Einstellungen sind Teil des Formulars &quot;<a href="#edit_member_group"><u>Mitgliedsgruppen ändern</u></a>&quot;.
Die folgenden Einstellungen sind verfügbar: 
<ul>
	<li><b>Zugängliche Kommunikationswege:</b> Kommunikationswege sind die Zugriffsmöglichkeiten 
	auf Cyclos. Hier können Sie wählen, welche Zugriffsmöglichkeiten auf Cyclos diese Gruppe hat. 
	Lesen Sie dazu bitte den Abschnitt <a href="${pagePrefix}settings#channels"><u>Kommunikationswege</u></a> 
	dieser Hilfe. Eine oder	mehrere der folgenden Optionen können Sie wählen. 
	<ul>
		<li><b>Internetzugang:</b> Zugang zu Cyclos über die normale Webseite eines Browsers. 
		<li><b>POS-Web Zahlungen:</b> <b>P</b>oint <b>O</b>f <b>S</b>-Zugang für Läden, die Zahlungen 
		über Cyclos ermöglichen. Ist diese Option gewählt, so bedeutet das, dass die Auftraggeber-Gruppe 
		(die Konsumenten) die Zugangseinstellung benötigt. 
		<li><b>SMS:</b> Dieses Modul befindet sich in der Entwicklung. 
		<li><b>WAP 1 und WAP 2 Zugang:</b> Zugang zu Cyclos über Mobiltelefon. 
		<li><b>Internetshop-Zahlungen:</b> Zugang über eine  &quot;Zahlung über Cyclos&quot; 
		Schaltfläche in einem Internetshop. 
	</ul>
	<br>
	<br>
	<li><b>Standard-Kommunikationswege:</b> Jedes Mitglied dieser Gruppe kann in Bezug auf 
	Kommunikationswege seine eigenen Präferenzen wählen. Alle im vorangegangenen Punkt 
	gewählten Elemente (&quot;Zugängliche Kommunikationswege&quot;) können vom Benutzer auf 
	seinen persönlichen Seiten ein oder ausgeschaltet werden. Wenn Sie z.B. im Auswahlfeld 
	Kommunikationswege &quot;Standard-Kommunikationswege&quot; das &quot;WAP 1&quot; wählen, 
	bedeutet dies für alle Mitglieder dieser Gruppe, dass der Kommunikationsweg &quot;WAP 1&quot; 
	möglich, und in den persönlichen Einstellungsseiten des Mitglieds als Standard eingestellt 
	ist. Allerdings ist jedes Mitglied frei, diese Einstellung für sich selbst wieder 
	auszuschalten.<br>
	<br>
	
	<li><b>PIN-Länge:</b> Für einige Kommunikationswege , wie z.B. SMS und Internetshop kann die 
	Verwendung einer PIN (nur Zahlen) eingestellt werden. In dieser Option können Sie die maximale 
	und die minimale Länge der PIN einstellen.<br>
	Bitte beachten Sie, dass ein Kommunikationsweg mit Verwendung von PIN gewählt sein muss, damit 
	diese Option erscheint. Beim Hinzufügen des ersten mit PIN zugänglichen Kommunikationswegs 
	müssen Sie zuvor die Gruppeneinstellungen speichern, bevor die PIN-Längen-Option erscheint.<br>
	<br>
	<li><b>Maximale Versuche ungültiger PIN:</b> (Nur sichtbar, wenn ein Kommunikationsweg für
	diese Gruppe verwendet wird, der einen PIN erfordert). Erreicht der Benutzer die maximale Anzahl 
	der Versuche, kann der Benutzer sich bis zum Ablauf der Sperrzeit nicht wieder anmelden 
	(siehe nächste Einstellung).<br>
	<br>
	<li><b>PIN nach zu vielen Versuchen vorübergehend gesperrt:</b> (Nur sichtbar, wenn ein 
	Kommunikationsweg für	diese Gruppe verwendet wird, der einen PIN erfordert). Dies ist der 
	Zeitraum, während dessen ein Benutzer sich nach Erreichen der maximalen Anzahl an PIN-Versuchen 
	nicht mehr anmelden kann.<br>
	<br>
	<li><b>Kennwortlänge:</b> bestimmt Mindest-und Maximallänge des vom Benutzer für die Anmeldung 
	benötigten Kennworts.<br>
	<br>
	<li><b>Kennwortüberwachung:</b> Hier können Sie sich für eine von drei leicht verständlichen 
	Optionen entscheiden.<br>
	<br>
	<li><b>Maximum Kennwortversuche:</b> Erreicht der Benutzer die maximale Anzahl der Versuche, kann 
	der Benutzer sich bis zum Ablauf der Sperrzeit nicht wieder anmelden (siehe nächste Einstellung).<br>
	<br>
	<li><b>Sperrzeit nach maximalen Kennwortversuchen:</b> Dies ist der Zeitraum, während dessen ein 
	Benutzer sich nach Erreichen der maximalen Anzahl an Kennwortversuchen nicht mehr anmelden kann.<br>
	<br>
	<li><b>Benutzerkennwort abgelaufen nach:</b> Mit dieser Einstellung können Sie den Zeitraum definieren, 
	während dessen das Benutzerkennwort gültig ist. Läuft der Zeitraum ab, so muss das Mitglied ein 
	neues Kennwort eingeben. Wenn Sie hier &quot;0&quot; eingeben, läuft das Kennwort niemals ab.<br>
	<br>
	<li><b>Überweisungskennwort:</b> Hier können Sie die Verwendung eines speziellen Kennworts für 
	Überweisungen einstellen. Sie haben die folgenden Optionen: 
	<ul>
		<li><b>&quot;Nicht verwendet&quot;</b>: Es werden keine Überweisungskennwörter verwendet, 
		und Mitglieder können jede Überweisung durchführen (falls sie die entsprechenden 
		Berechtigungen haben), ohne zuerst ein Überweisungskennwort einzugeben. 
		<li><b>&quot;Automatisch&quot;</b>: Wird die Option &quot;Automatisch&quot; gewählt, 
		generiert das System bei Einrichtung des Kontos eines neuen Mitglieds automatisch ein 
		Überweisungskennwort (oder ab jetzt, für bereits existierende Mitglieder). Das Mitglied 
		erhält das Kennwort (nur einmal) in seinem persönlichen Nachrichtenbereich. 
		<li><b>&quot;Manuell&quot;</b>: Wird dies gewählt, kann das Überweisungskennwort nur 
		manuell generiert werden, über <a href="${pagePrefix}profiles#access_actions">
		<u>&quot;Überweisungskennwort verwalten&quot;</u></a> im Profil unter Mitglieds-Aktionen.
		Mehr Information zum Überweisungskennwort finden Sie auf der Seite. 
	</ul>
	<br>
	<li><b>Überweisungskennwortlänge:</b> Einstellen der Länge des Überweisungskennworts. 
	Dieses Kennwort hat stets eine festgelegte Länge.<br>
	<br>
	<li><b>Maximale Versuche Überweisungskennwort:</b> Nach dieser Anzahl fehlgeschlagener 
	Versuche wird das Kennwort blockiert. Ein Administrator kann das Kennwort über  
	<a href="${pagePrefix}profiles#access_actions"><u>&quot;Überweisungskennwort verwalten&quot;</u></a> 
	zurücksetzen.
</ul>
<hr class="help">
</span>

<span class="admin"> <a name="group_notification_settings"></a>
<h3>Benachrichtigungseinstellungen</h3>
Diese Einstellungen betreffen die persönlichen Benachrichtigungen, die die Mitglieder 
einer Gruppe aktivieren können. 
<ul>
	<li><b>Standardnachrichten mit E-Mail versenden:</b> Dies sind die bei der Einrichtung 
	eines Mitglieds (für diese Gruppe) als Standard markierten Benachrichtigungen.<br>
	Wenn Sie in diesem Auswahlfeld ein Element wählen, so ist dies in den persönlichen 
	Benachrichtigungsoptionen eines jeden Mitglieds dieser Gruppe als Standard markiert. 
	Allerdings kann jedes Mitglied selbst diese Benachrichtigung abwählen. 
	<li><b>SMS-Nachrichten:</b> Dies sind die Nachrichten, die für SMS-Nachrichten verfügbar 
	sind.<br> 
	Anmerkung: Diese Option ist nur dann verfügbar, wenn SMS-Kommunikationsweg aktiviert wurde 
	(Basiseinstellungen – Kommunikationswege). 
	<li><b>Standardnachrichten per SMS senden:</b> Dies sind die bei der Einrichtung eines 
	Mitglieds (für diese Gruppe) als Standard markierten Benachrichtigungen. Wenn Sie in diesem 
	Auswahlfeld ein Element wählen, so ist dies in den persönlichen Benachrichtigungsoptionen 
	eines jeden Mitglieds dieser Gruppe als Standard markiert. Allerdings kann jedes Mitglied 
	selbst diese Benachrichtigung abwählen.<br>
	<li><b>SMS-Gebühr Überweisungstyp:</b> Dieser Überweisungstyp wird für den Beitrag für 
	ausgehende SMS verwendet (System an Mitglied, z.B. für Benachrichtigungen). 
	<li><b>SMS-Gebühr:</b> Dies ist der Betrag, der für jede ausgehende SMS erhoben wird. 
</ul>
Die meisten Elemente auf dieser Liste sind recht einfach und erklären sich von selbst. Diese Hilfe 
geht daher nur auf diejenigen Elemente ein, die der zusätzlichen Erklärung bedürfen.<br>
Die Hilfe für <a href="${pagePrefix}notifications#notification_preferences"><u>Benachrichtigungsoptionen</u></a> 
enthält weitere Informationen zu den spezifischen Optionen.

<hr class="help">
</span>

<span class="admin">
<a name="group_ad_settings"></a>
<h3>Inserate-Einstellungen</h3>
Inserateinstellungen sind Einstellungen, die das Verhalten der Gruppe in Zusammenhang mit 
Inseraten definieren. Die Einstellungen sind Teil des Formulars  
&quot;<a href="#edit_member_group"><u>Mitgliedsgruppen ändern</u></a>&quot;. 
Die folgenden Einstellungen sind verfügbar: 
<ul>
	<li><b>Maximale Anzahl Inserate je Mitglied:</b> Erklärt sich von selbst.<br>
	<br>
	<li><b>Dauerinserate freigeben:</b> Wird diese Option gewählt, sind in dieser Mitgliedsgruppe  
	<a href="${pagePrefix}advertisements#ad_modify"><u>Dauerinserate</u></a> möglich.<br>
	<br>
	<li><b>Standard Inserat-Veröffentlichungsdauer:</b> Die als Standard eingestellte Dauer eines 
	neuen Inserats. Dem Mitglied steht es stets frei, den Aktivierungszeitraum für jedes Inserat 
	neu einzustellen.<br>
	<br>
	<li><b>Maximale Inserat-Veröffentlichungsdauer:</b> Der maximale Zeitraum, während dessen ein 
	Mitglied sein Inserat veröffentlichen kann.<br>
	<br>
	<li><b>Externe Inserat-Veröffentlichung:</b> Dies ermöglicht die externe Veröffentlichung von 
	Inseraten, was bedeutet, dass alle Inserate dieser Gruppe automatisch auf der Website der 
	Organisation veröffentlicht werden können, wo sie auch von Nicht-Mitgliedern gelesen werden können. 
	Alle Optionen erklären sich selbst. &quot;Auswahl zulassen&quot; bedeutet, das das Mitglied selbst 
	wählen kann, ob seine Inserate für die externe Veröffentlichung verwendet werden können.<br>
	<br>
	<li><b>Maximale Anzahl Bilder je Inserat:</b> Die maximale Anzahl der Bilder, die ein Mitglied 
	in ein Inserat aufnehmen kann.<br>
	<br>
	<li><b>Max. Länge der Bescheibung des Inserates:</b> Die maximale Länge des Beschreibungstextes in 
	Inseraten, in Zeichen.
</ul>
<hr class="help">
</span>

<a name="group_scheduled_payment_settings"></a>
<h3>Einstellungen geplante Zahlungen der Gruppe</h3>
Die sind die Einstellungen in Zusammenhang mit  <a href="${pagePrefix}payments#scheduled"><u>
geplante Zahlungen</u></a>. Die Einstellungen sind Teil des Formulars  
&quot;<a href="#edit_member_group"><u>Mitgliedsgruppe ändern</u></a>&quot;.
<p>
All diese Einstellungen beziehen sich auf <a href="${pagePrefix}payments#pay_scheduled"><u>
mehrfache geplante Zahlungen</u></a>. 
Die folgenden Einstellungen sind verfügbar: 
<ul>
	<li><b>Max. geplante Zahlungen:</b> Die maximale Anzahl der geplanten Teilzahlungen, in die eine 
	Zahlung unterteilt werden kann. Zum Beispiel: 10 Teilzahlungen, eine alle zwei Wochen. 
	<li><b>Max. Planungszeitraum:</b> Der maximale Gesamtzeitraum zwischen dem heutigen Tag und der 
	letzten Zahlung. 
</ul>
<hr class="help">


<span class="admin">
<a name="group_loans_settings"></a>
<h3>Darlehenseinstellungen der Gruppe</h3>
Die sind die Einstellungen in Zusammenhang mit  <a href="${pagePrefix}loans"><u>Darlehen</u></a>.
Die Einstellungen sind Teil des Formulars &quot;<a href="#edit_member_group"><u>Mitgliedsgruppen ändern</u></a>&quot;. 
Die folgenden Einstellungen sind verfügbar: 
<ul>
	<li><b>Darlehen der Darlehensgruppe anzeigen:</b> Das Mitglied kann seine eigenen <a
		href="${pagePrefix}loans"><u>Darlehen</u></a> einer <a
		href="${pagePrefix}loan_groups"><u>Darlehensgruppe</u></a> anzeigen.
	<li><b>Darlehenrückzahlung erlaubt durch jedes Mitglied der Darlehensgruppe:</b> Wird 
	diese Option gewählt, kann jedes Mitglied der Darlehensgruppe ein Darlehen zurück zahlen. 
	Wird die Option nicht gewählt, kann nur das als &quot;verantwortliches Mitglied&quot; 
	markierte Mitglied das Darlehen zurück zahlen. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="group_brokering_settings"></a>
<h3>Brokering-Einstellungen der Brokergruppe</h3>
Dies sind die Einstellungen in Zusammenhang mit den 
<a href="${pagePrefix}brokering"><u>Brokering</u></a>-Funktionen eines Brokers. Die Einstellungen sind 
Teil des Formulars &quot;<a href="#edit_member_group"><u>Mitgliedsgruppen ändern</u></a> &quot;. 
Die folgenden Einstellungen sind verfügbar: 
<ul>
	<li><b>Mögliche Startgruppen:</b> Hier können Sie die Startgruppe für die von einem 
	Broker registrierten Mitglieder wählen. Dies hängt ab von der Art der Verwendung der Broker. 
	Möglich wäre eine inaktive Gruppe, wie die Interessenten-Gruppe, die noch der Freigabe durch 
	einen Administrator bedürfen, oder aber eine aktive Gruppe.<br>
	Wenn Sie hier mehr als eine Gruppe wählen, so erscheint für den Broker eine Liste der Gruppen, 
	zwischen denen er wählen kann.<br>
	Bitte beachten Sie, dass der Broker KEINE Mitglieder registrieren kann, solange Sie hier nicht 
	eine oder mehrere Gruppen gewählt haben. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="edit_broker_group"></a>
<h3>Brokergruppe ändern</h3>
Hier können Sie die Eigenschaften einer 
<a href="#broker_groups"><u>Brokergruppe</u></a> bearbeiten. Nach Anklicken der 
Schaltfläche &quot;Bearbeiten&quot;, können Sie den Namen, die Beschreibung und einige 
andere Einstellungskategorien ändern.<br>
<p>
Bitte beachten Sie, dass Sie hier keine Berechtigungen einstellen können. Gruppeneigenschaften 
und Einstellungen sind nicht das Gleiche wie Berechtigungen. Durch Anklicken des 
Berechtigungs-Symbols <img border="0" src="${images}/permissions.gif" width="16"
	height="16"> in der <a href="#manage_groups"><u>Gruppen-Übersicht</u></a>, können Sie die 
	Gruppenberechtigungen ändern, Sie können allerdings auch die Schaltfläche mit der 
	Beschriftung &quot;Gruppenberechtigungen&quot; unter dem Fenster benutzen.<br>
<p>
Einstellungen der Brokergruppen sind nach Kategorien geordnet. Die folgenden Kategorien sind 
verfügbar; klicken Sie auf die Links, um mehr Details zu den Feldern dieser Kategorien zu 
erhalten. 
<ul>
	<li><b><a href="#group_details"><u>Gruppendetails</u></a></b> zeigt eine Zusammenfassung. 
	Nach Anklicken der Schaltfläche &quot;Bearbeiten&quot;, können Sie den Namen, die Beschreibung 
	und einige andere Einstellungskategorien ändern. 
	<li><b><a href="#group_registration_settings"><u>Registrierungseinstellungen</u></a></b> 
	sind Einstellungen, die das Verhalten der Gruppe in Zusammenhang mit Mitgliedsregistrierung definieren. 
	Hier sind auch verschiedene andere Einstellungen beinhaltet. 
	<li><b><a href="#group_access_settings"><u>Zugangseinstellungen</u></a></b> sind 
	Einstellungen, die den Zugang der Gruppe definieren .
	<li><b><a href="#group_notification_settings"><u>Benachrichtigungseinstellungen</u></a></b> 
	behandeln die E-Mail-Benachrichtigungen an diese Gruppe.
	<li><b><a href="#group_brokering_settings"><u>Brokering-Einstellungen</u></a></b> sind 
	Einstellungen in Zusammenhang mit dem Brokering anderer Mitglieder.
	<li><b><a href="#group_ad_settings"><u>Inserate-Einstellungen</u></a></b>
	sind Einstellungen, die das Verhalten der Gruppe in Zusammenhang mit Inseraten definieren.
	<li><b><a href="#group_loans_settings"><u>Darlehenseinstellungen</u></a></b> sind Einstellungen, 
	die das Verhalten der Gruppe in Zusammenhang mit Darlehen definieren. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="insert_group"></a>
<h3>Neue Gruppe einfügen</h3>
Dieses Fenster ermöglicht Ihnen, eine neue Gruppe einzurichten. 
<br>
Sie haben die folgenden Optionen: 
<ul>
	<li><b>Typ:</b> Der <a href="#group_categories"><u>Typ</u></a> der Gruppe.
	Hier kann es sich um &quot;Mitglied&quot;, &quot;Broker&quot; oder 
	&quot;Administrator&quot; handeln. 
	<li><b>Entfernt:</b> Ist eine Gruppe als &quot;entfernt&quot; markiert, so bedeutet das, 
	dass die Mitglieder dieser Gruppe das System bereits verlassen haben. Ist ein Mitglied 
	erst einmal in der Gruppe &quot;Gelöschte Mitglieder&quot;, kann es niemals in irgendeine 
	andere Gruppe zurück kehren. Die Daten werden in der Datenbank gespeichert, und können von 
	Administratoren eingesehen werden, haben allerdings nur noch Backup-Funktion.  
	<li><b>Name:</b> Name der Gruppe, so wie er den Benutzern erscheint. 
	<li><b>Beschreibung:</b> Beschreibung der Gruppe. 
	<li><b>Kopiere Einstellungen von:</b> Über dieses Auswahlfeld können Sie alle Einstellungen 
	für diese neue Gruppe von einer bereits bestehenden kopieren. Sowohl Einstellungen als
	 auch Berechtigungen werden kopiert. 
</ul>
Nach Eingabe der Information müssen Sie natürlich auf &quot;Weiter&quot; klicken um die 
Änderungen zu speichern.<br>
<b>Wichtig:</b>
Nach Einrichten der neuen Mitgliedsgruppe sollten Sie die Berechtigungen und Eigenschaften 
über die Seite &quot;Gruppen&quot; einstellen. 
<p>
Anmerkung: Nach Einrichten einer Gruppe ist es nicht mehr möglich, Typ und Status der Gruppe 
zu ändern. 
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_accounts"></a>
<h3>Gruppenkonten verwalten</h3>
<a href="#member_groups"><u>Mitgliedergruppen</u></a>
(und
<a href="#broker_groups"><u>Brokergruppen</u></a>
) können verschiedene 
<a href="${pagePrefix}account_management#accounts"><u>Konten</u></a> zugeordnet 
werden. Die Liste unten zeigt die Mitgliedskonten, die der Gruppe zugeordnet sind. 
Die Konten in der Liste erscheinen in der persönlichen Kontenübersicht für die 
Mitglieder dieser Gruppe. Ein Kontentyp kann von verschiedenen Gruppen geteilt 
werden, d.h. verschiedene Gruppen können den gleichen zugeordneten Kontentyp haben. 
In einem solchen Fall können Sie aber immer noch für jede Gruppe unterschiedliche 
Einstellungen und Berechtigungen definieren. 
<p>
Konteneinstellungen können Sie durch Anklicken des Bearbeiten-Symbols <img border="0"
	src="${images}/edit.gif" width="16" height="16">&nbsp;ändern.<br>
<b>ANMERKUNG</b>: Bitte denken Sie daran, dass Einrichten und Verändern noch nicht 
ausreichen, damit ein Konto Zahlungen empfangen und überweisen kann. Ebenso müssen Sie 
die <a href="#permissions"><u>Berechtigungen</u></a> für die Gruppe einstellen, andernfalls 
kann das Konto nicht genutzt werden. 
<p>
Einen Kontentyp von der Gruppe &quot;lösen&quot; können Sie durch Anklicken des 
Löschen-Symbols <img	border="0" src="${images}/delete.gif" width="16" height="16">&nbsp;. 
Wenn Sie ein Konto von einer Gruppe &quot;lösen&quot;, so sind das Konto und alle seine 
Überweisungen für die Benutzer zwar weiterhin sichtbar, das Konto ist allerdings im 
&quot;inaktiven&quot; Status. Es kann nicht mehr für Zahlungen verwendet werden. 
<p>
Sie können der Gruppe auch einen neuen Kontotyp zuordnen, indem Sie die Schaltfläche 
&quot;Neues Konto zuordnen&quot; unter diesem Formular anklicken. Dies bringt Sie zum Fenster
 <a href="#insert_group_account"><u>&quot;Gruppenkonto hinzufügen&quot;</u></a>.
<hr class="help">
</span>

<span class="admin">
<a name="edit_group_account"></a>
<a name="insert_group_account"></a>
<h3>Der Gruppe ein Konto hinzufügen / Kontoeinstellungen ändern</h3>
Auf dieser Seite können Sie die Kontoeinstellungen für einen Kontotyp der gewählten Gruppe 
definieren. Dies ist möglich für einen bereits existierenden, zugeordneten Kontotyp, oder 
aber für einen Kontotyp, den Sie der Gruppe zum ersten mal zuordnen möchten. 
<br>
Beim Ändern eines existierenden Kontos, müssen Sie auf &quot;Bearbeiten&quot; klicken, um 
Änderungen vornehmen zu können; beim Hinzufügen eines neuen Kontotyps können Sie Ihre 
Eingaben direkt in den Feldern machen. Nachdem Sie dies getan haben, klicken Sie bitte auf 
&quot;Weiter&quot; um die Änderungen zu speichern. 
<br>
Die folgenden Einstellungen können definiert werden: 
<ul>
	<li><b>Konto:</b> Spezifizieren Sie hier bitte, welchen Kontotyp Sie zuordnen möchten. Dies 
	kann nach Einrichtung des Kontotyps nicht mehr geändert werden, ist also nur für neue Kontotypen 
	verfügbar. Einen Kontotyp können Sie erst dann zuordnen, wenn Sie ihn zuvor eingerichtet haben; 
	falls Sie den entsprechenden Kontotyp noch nicht eingerichtet haben, sollten Sie dies zuvor 
	tun. 
	<li><b>Ist Standard:</b> Da es möglich ist, mehr als ein Mitgliedskonto einzurichten, muss das 
	System wissen, welches das als Standard eingestellte Konto ist. Dafür gibt es zwei Gründe: Zum 
	Ersten kann das System so eingestellt werden, dass es an das Mitglied E-Mails zur Information 
	über seinen Kontostand (auf dem als Standard eingestellten Konto) schickt. Zweitens verwenden 
	per Mobiltelefon getätigte Zahlungen das als Standard eingestellte Konto. 
	<li><b>Überweisungskennwort erforderlich:</b> Wir diese Option gewählt, wird das Mitglied 
	aufgefordert, das Überweisungskennwort einzugeben.<br> 
	Anmerkung: Diese Option ist nur dann verfügbar, wenn das Überweisungskennwort für die Gruppe 
	freigegeben wurde (in Gruppeneinstellungen). Andernfalls erscheint sie nicht.
	<li><b>Konto ausblenden, wenn kein Kreditlimit:</b> Für manche Finanzprodukte 
	möchten Sie möglicherweise nicht, dass ein Mitgliedskonto erscheint, 
	falls kein Kreditlimit definiert wurde. Dies kann der Fall sein, 
	wenn ein zusätzliches Mitgliedskonto für Zahlungen per Kreditkarte 
	verwendet wird. Normalerweise hat ein solches Konto ein negatives 
	Kreditlimit, das das Mitglied für Kartenzahlungen verwenden kann. 
	Dem Mitglied werden dann regelmäßig Beträge zur Rückzahlung dieser 
	Schuld berechnet. Möglicherweise haben nicht alle Mitglieder ein 
	individuelles negatives Kreditlimit, und Sie möchten dass das Kreditkonto 
	dieser Mitglieder nicht erscheint<br>
	Dies kann natürlich durch die Einrichtung einer separaten Gruppe 
	&quot;Mitglieder Kreditkarten&quot; gelöst werden, und indem das Kreditkonto 
	einer neuen Gruppe zugewiesen wird, allerdings erhöht es die Komplexität des 
	Ganzen. Um zu vermeiden, dass unterschiedliche Gruppen eingerichtet werden 
	müssen, ist es möglich Konten nur dann erscheinen zu lassen wenn die entsprechenden 
	Mitglieder negative Kreditlimits haben. Natürlich trifft dies auf individuelle 
	Kreditlimits zu. Hat die gesamt Gruppe ein unteres Kreditlimit, so erscheint das 
	Konto für alle und das Einstellen dieser Option erübrigt sich.<br>
  Anmerkung: Generell gilt, dass alle Konten mit Überweisungen in Cyclos
  erscheinen. Das bedeutet, wenn diese Option für ein Konto eingestellt 
  wurde und das Mitglied zwar kein Kreditlimit aber Überweisungen auf diesem 
  Konto hat, so erscheint das Konto und diese Option wird dadurch augehoben. 
	<li><b>Unteres Kreditlimit:</b> Der für das Konto als Standard eingestellte untere Kreditlimit. 
	Typischerweise handelt es sich hier um 0, oder einen Minusbetrag. Dies zu ändern kann Auswirkungen 
	auf bestehende Konten haben, abhängig vom Kontrollkästchen &quot;Kreditlimits für existierende 
	Mitglieder aktualisieren&quot;, etwas weiter unten auf dieser Seite. Zusätzlich zum 
	Standard-Kreditlimit, der für die ganze Gruppe gilt, ist es auch möglich, einen <a 
	href="${pagePrefix}account_management#credit_limit"><u>Kreditlimit</u></a> 
	für einzelne Mitglieder einzustellen. Die persönliche Einstellung überschreibt dann den 
	Gruppenkreditlimit. 
	<li><b>Oberes Kreditlimit:</b> Wenn Sie einen oberen Kreditlimit definieren, bedeutet dies, dass 
	die Konten keine Zahlungen mehr erhalten, wenn dieser erreicht ist. Der Auftraggeber erhält 
	dann eine Nachricht, dass der Empfänger nicht in der Lage ist, Zahlungen zu empfangen.
	<li><b>Kreditlimits für existierende Mitglieder aktualisieren:</b> Wenn Sie die Kreditlimits einer 
	bestehenden Kontentyp-Zuordnung geändert haben, können Sie mit dieser Auswahl definieren, ob die 
	Kreditlimits für die bestehenden Mitglieder (entweder einzeln oder als Gruppen-Kreditlimits) 
	geändert werden sollen. Wenn Sie diese Kästchen nicht wählen, wird der neue Kreditlimit nur auf 
	neue Mitglieder dieser Gruppe angewandt. Dieses Kästchen erscheint nicht, wenn Sie diesen Kontotyp 
	zum ersten Mal zuordnen. 
	<li><b>Startguthaben:</b> Dies ist das Startguthaben, das alle neuen Mitglieder automatisch erhalten. 
	Dies kann 0 betragen, oder einen positiven Betrag. 
	<li><b>Startguthaben Überweisungstyp:</b> Ist das in der Bearbeitungsbox direkt über dieser eingestellte 
	Startguthaben ein anderer Betrag als 0, müssen Sie in diesem Auswahlfeld den
	<a href="${pagePrefix}account_management#transaction_types"><u>Überweisungstyp</u></a> für dieses 
	Guthaben festlegen. Da der Überweisungstyp ein Ursprungs- und ein Zielkonto hat, ermöglicht Ihnen 
	dies, festzulegen, von welchem Konto das Startguthaben kommt. Der als Standard eingestellte   
	Überweisungstyp &quot;Startguthaben&quot; ist vom Debitkonto (Ausgangskonto) auf das Mitgliedskonto 
	ausgestattet,	aber natürlich steht es Ihnen frei, einen anderen Überweisungstyp zu verwenden.
	<li><b>Meldung niedriger Kontostand:</b> Erreicht ein Mitgliedskonto diesen Betrag, wird eine 
	persönliche Nachricht (siehe unten) versandt. Hier können Sie einen positiven Betrag eingeben. Der 
	von Ihnen eingegebene Betrag steht in Relation zum Kreditlimit. Die bedeutet z.B., wenn der 
	Kreditlimit auf –200 eingestellt ist, mit einer Einstellung &quot;Niedriger Kontostand&quot; von 10, 
	so erhält das Mitglied eine Meldung zum niedrigen Einheitenstand, sobald der Kontostand –190 beträgt. 
	Ist der Kreditlimit 0, und die Einstellung &quot;Niedriger Kontostand&quot; ist 10, so erhält das Mitglied 
	eine Meldung, sobald der Saldo 10 (positive) Einheiten beträgt. 
	<li><b>Nachricht bei niedrigem Kontostand:</b> Die (persönliche) Benachrichtigung, die ein Mitglied erhält 
	wenn der eingestellte niedrige Kontostand erreicht ist. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_customized_files"></a>
<h3>Benutzerdefinierte Dateien verwalten</h3>
Cyclos lässt sich in hohem Maße benutzerdefinieren. Sie können nicht nur Ihre 
eigenen Styles und Texte definieren (über <a href="${pagePrefix}content_management"><u>
&quot;Content Management&quot;</u></a>), sondern Sie können dies auch auf der Gruppenebene 
spezifizieren. Dies bedeutet, dass jede Gruppe ihren eigenen Stil und ihre eigenen 
spezifischen Texte haben kann, die dann in der Software erscheinen. Diese werden 
&quot;benutzerdefinierte Dateien&quot; genannt. 
<p>
Dieses Fenster zeigt eine Übersicht der benutzerdefinierten Dateien für diese dieser Gruppe. 
Wurden noch keine Dateien benutzerdefiniert, erscheint eine Nachricht mit dem Inhalt, dass 
noch keine verfügbar sind. Sie haben die folgenden Optionen: 
<ul>
	<li><b>Einfügen</b> eine neuen benutzerdefinierten Datei über die Schaltfläche &quot;Neue 
	Datei anpassen&quot;. Eine benutzerdefinierte Gruppen-Datei überschreibt die vom System angepasste 
	Datei (diese können Sie in <a href="${pagePrefix}content_management"><u>&quot;Content
	Management&quot;</u></a>) einrichten.
	<li><b>Ändern</b> einer bestehenden benutzerdefinierten Datei über das Bearbeiten-Symbol 
	<img border="0"	src="${images}/edit.gif" width="16" height="16">.
	<li><b>Anzeigen</b> wie das Ergebnis für ein Mitglied der Gruppe aussieht, über das 
	Anzeigen-Symbol <img border="0" src="${images}/view.gif" width="16" height="16">.
	<li><b>Löschen</b> einer benutzerdefinierten Datei über das Löschen-Symbol <img border="0"
		src="${images}/delete.gif" width="16" height="16">.
	Wird eine benutzerdefinierte Datei gelöscht, wird danach die als Standard eingestellte Systemdatei 
	verwendet, falls verfügbar. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="customize_group_file"></a>
<h3>Ändern einer benutzerdefinierten Datei für... / Neue Datei anpassen für...</h3>
Auf dieser Seite können Sie eine statische Datei oder eine CSS-Datei anpassen. Das funktioniert 
genau so wie in <a href="${pagePrefix}content_management"><u>Content Management</u></a>, 
allerdings gelten diese Spezifizierungen nur für diese Gruppe. 
<br>
Für genauere Informationen zu den notwendigen Eingaben, lesen Sie bitte 
<a href="${pagePrefix}content_management#edit_customized_file"><u>hier</u></a>.
<p>
Anmerkung: Möchten Sie der statischen Datei <a href="${pagePrefix}content_management#custom_images">
<u>Bilder hinzufügen</u></a>, müssen Sie diese zuvor hochladen.
<hr class="help">
</span>

<p>
<h2>Berechtigungen</h2>
</p>
<p></p>
<hr>

<a name="permissions"></a>
<p class="sub_description">
In Cyclos sind die Berechtigungen in den Gruppen organisiert. Die Zugangsberechtigungen für 
die Funktionen der Cyclos-Software können Sie für jede Gruppe verwalten. Normalerweise 
ist es so, dass die Cyclos-Abschnitte, zu denen eine Mitgliedsgruppe keinen Zugang hat, 
für diese Gruppe auch nicht sichtbar sind. 
<br>
<span class="admin">
Das System ist sehr flexibel. Es ermöglicht Ihnen z.B. das Einrichten verschiedener 
Administratorengruppen, wobei jede Administratorengruppe die Berechtigungen zum Verwalten 
einer bestimmten Mitgliedsgruppe hat. Für komplexere Systeme ist es möglich, zusätzlich 
Administratorengruppen einzurichten, um spezifische Augabenbereiche wie Kontoadministrator,  
Systemadministrator, Buchhaltungsadministrator, Administratoren von Gutscheinen/Scrips etc. 
einzurichten. 
</p>
Gruppenberechtigungen ändern können Sie über  &quot;Menü: Benutzer und Gruppen > Gruppen&quot;, 
und Anklicken des Berechtigungs-Symbols <img border="0" src="${images}/permissions.gif" width="16" height="16">.
</span>
<hr class="help">

<a name="manage_group_permissions_basic"></a>
<h3>Grundlegende Gruppenberechtigungen:</h3>
In diesem Fenster können Sie die grundlegenden Berechtigungen einstellen. Dies 
sind Berechtigungen, die sich für alle <a href="#group_categories"><u>Gruppentypen</u></a>
gleichen (Mitglied, Administrator, Broker). 
<br>
Diese Basiseinstellungen haben keine Auswirkung auf andere Funktionen, wenn z.B. ein 
Mitglied sich nicht anmelden kann, kann es doch möglicherweise weiterhin Zahlungen erhalten. 
<br>
Die folgenden Berechtigungen können eingestellt werden: 
<ul>
	<li><b>Anmelden:</b> Ist hier kein Häkchen gesetzt, können sich Mitglieder dieser 
	Gruppe nicht anmelden. 
	<li><b>Mitglied einladen:</b> Ist hier ein Häkchen gesetzt, so sehen Mitglieder dieser 
	Gruppe eine Fensterbox auf ihrer Hauptseite (nach der Anmeldung), mit der sie einen Freund 
	dazu einladen können, <a href="${pagePrefix}home#home_invite">
	<u>Ihre Organisation kennen zu lernen</u></a>. 
	<li><b>Schnellzugriff anzeigen:</b> Diese Option (nur für Mitglieds- und Brokergruppen) 
	lässt auf der Startseite ein Fenster mit Schnellzugangs-Symbolen zu den meisten 
	Funktionen erscheinen. 
</ul>
<hr class="help">

<span class="admin">
<a name="manage_group_permissions_admin_system"></a>
<h3>Berechtigungen Systemadministratoren</h3>
In diesem Fenster können Sie die Berechtigungen für die Systemfunktionen einer 
Administratorengruppe einstellen. Dieses Berechtigungsfenster enthält also keine 
Berechtigungen in Verbindung mit Mitgliedsgruppen. Um Änderungen vornehmen zu können 
müssen Sie zum unteren Ende der Seite scrollen, und dort die Schaltfläche &quot;Bearbeiten&quot; 
wählen. Die Änderungen speichern Sie durch Anklicken von &quot;Weiter&quot;, ganz 
unten auf der Seite. 
<p>
Die Berechtigungsstruktur ist recht einfach. Die meisten Funktionen haben zwei Berechtigungen, 
&quot;Ansicht&quot; und &quot;Verwalten&quot;.<br> 
Wird Ansicht nicht gewählt, so erscheint das Element im Menü der Administratoren dieser Gruppe
auch nicht.<br>
Die Option &quot;Verwalten&quot; berechtigt zum Einrichten, Bearbeiten und Löschen.<br> 
Die folgenden Berechtigungen sind verfügbar (für mehr Information wenden Sie sich bitte 
an die Links): 
<ul>
	<li><b>Kontogebühren:</b> Die Berechtigung &quot;Ansicht&quot; ermöglicht dem Administrator 
	auf die Seite <a href="${pagePrefix}account_management#account_fee_overview">
	<u>Übersicht Kontogebühren</u></a> zu gehen.<br>
	Die Berechtigung &quot;Belastung&quot; ermöglicht es dem Administrator Kontogebühren &quot;Manuell&quot; 
	zu starten oder fehlgeschlagene Kontogebühren im 
	<a href="${pagePrefix}account_management#account_fee_history"><u>Kontogebühr-Verlauf</u></a> 
	zu erheben.<br>
	<br>
	<li>Die <b>Konten:</b> in diesem Abschnitt können Sie die folgenden Berechtigungen einstellen: 
	<ul>
		<li><b><a href="${pagePrefix}account_management#account_search"><u>Konten 
		verwalten</u></a></b>	Berechtigung zum Einrichten und Ändern der Kontenstruktur.
		<li><b>Kontoverwaltung anzeigen:</b> wie oben, allerdings kann der Administrator 
		keine Veränderungen vornehmen.
		<li><b>Systemkonten-Information anzeigen:</b> Hier können Sie einstellen, welche 
		Systemkonto-Zusammenfassungen für den Administrator sichtbar sind.
		<li><b><a href="${pagePrefix}payments#authorized"><u>Autorisierte Zahlungen</u></a></b> anzeigen. 
		Menüelement autorisierte Zahlungen anzeigen. 
		<li><b><a href="${pagePrefix}payments#scheduled"><u>Geplante Zahlungen</u></a></b> anzeigen.
		Menüelement geplante Zahlungen anzeigen. 
	</ul>
	<br>
	<br>
	<li><b>Inseratekategorien:</b> Einstellungen für die  <a
		href="${pagePrefix}advertisements#categories"><u>Inserat-Kategorien</u></a>.<br>
	<br>
	<li><b>Administratorengruppen:</b>
	<ul>
		<li>Wird <b>Ansicht</b> gewählt, kann der Administrator die verschiedenen 
		Administratorengruppen in seinem Fenster <a href="#manage_groups"><u>Gruppenberechtigungen</u></a>
		sehen.
		<li><b>Benutzerdefinierte Dateien verwalten:</b> ermöglicht dem Administrator, für andere 
		Administratoren <a href="#manage_group_customized_files"><u>benutzerdefinierte Dateien</u></a> 
		zu verwalten.
	</ul>
	<br>
	<br>
	<li><b><a href="${pagePrefix}alerts_logs#system_alerts"><u>Meldungen</u></a></b>
	sind vom System versandte Warnungen, zu besonderen Ereignissen.<br>
	<br>
	<li><b><a href="${pagePrefix}settings#channels"><u>Kommunikationswege</u></a></b> definieren, 
	über welches Medium der Benutzer auf Cyclos zugreift (z.B. Internet, Handy, etc).<br>
	<br>
	<li><b><a href="${pagePrefix}account_management#currencies"><u>Währungen:</u></a></b>
	ermöglicht dem Administrator, in Cyclos verschiedene Währungen anzuzeigen und/oder zu 
	verwalten.<br>
	<br>
	<li><b><a href="${pagePrefix}custom_fields"><u>Benutzerdefinierte Felder</u></a></b> ermöglicht 
	dem Administrator, benutzerdefinierte Felder zu verwalten.<br>
	<br>
	<li><b><a href="${pagePrefix}content_management#custom_images"><u>Benutzerdefinierte 
	Bilder</u></a></b> ermöglicht dem Administrator, benutzerdefinierte Bilder zu verwalten. 
	Dies beeinflusst die Sichtbarkeit der folgenden Menüelemente unter dem Hauptmenü 
	&quot;Content Management&quot;: 
	<ul>
		<li>Systembilder
		<li>Benutzerdefinierte Bilder
		<li>CSS-Bilder
	</ul>
	<br>
	<br>
		<li><b><a href="${pagePrefix}content_management"><u>
	Benutzerdefinierte Dateien:</u></a></b> ermöglicht dem Administrator, benutzerdefinierte 
	Dateien auf systemweiter Ebene einzustellen (im Gegensatz zu &quot;benutzerdefinierte Datei 
	pro Gruppe&quot;). Dies beeinflusst die folgenden Menüelemente unter dem Hauptmenü 
	&quot;Content Management&quot;: 
	<ul>
		<li>Statische Dateien	
		<li>Hilfedateien
		<li>CSS-Dateien
		<li>Anwendungsdateien
	</ul>
	<br><br>
	
	<li><b><a href="${pagePrefix}alerts_logs#error_log"><u>Fehlerprotokoll</u></a></b> 
	Ansicht und Verwalten des Fehlerprotokolls.<br>
	<br>
	<li><b><a href="${pagePrefix}bookkeeping"><u>Externe Konten (Buchhaltung):</u></a></b> 
	ermöglicht dem Administrator, das Modul zu konfigurieren, sowie Einrichten und Ändern von externen Konten und 
	der Definition von Feldern und Überweisungstypen.<br>
	Die Berechtigung zum Anzeigen ermöglicht das Anzeigen	von externen Kontokonfigurationen.<br>
	Die Detail-Berechtigung erlaubt Ihnen, die Zahlungen zu zeigen, aber an Ihnen keine Aktionen 
	durchzuführen.<br> 
	Die anderen Berechtigungen (Verarbeiten, Prüfen, und Verwalten) sind funktionsbedingte,
	Berechtigungen für externe Zahlungen.<br>
	<br>
	<li><b><a href="#group_filters"><u>Gruppenfilter:</u></a></b> Ermöglichen Ihnen, eine Reihe von 
	Gruppen	zu bündeln und diesen Gruppen einen spezifischen Namen zu geben. Auf diese Weise können 
	Sie eine Art &quot;Supergruppe&quot; einrichten, die in Cyclos für mehrere Zwecke verwendbar 
	ist.<br>
	Natürlich können Sie bei der Auswahl der Berechtigung &quot;Verwalten&quot; oder &quot;Ansicht&quot; 
	wählen, eine weitere Option ist &quot;Benutzerdefinierte Dateien verwalten&quot;. Dies würde dem 
	Administrator ermöglichen, benutzerdefinierte Dateien auf einen Gruppenfilter einzustellen, 
	also für eine ganze Reihe von Gruppen auf einmal.<br>
	<br>
	
	<li><b><a href="${pagePrefix}groups"><u>Gruppen:</u></a></b>
	ermöglicht es, verschiedene <a href="#group_categories"><u>Gruppenkategorien</u></a> 
	zu  <a href="#manage_groups"><u>verwalten</u></a>.<br>
	<br>
	
	<li><b>Sicherheitstypen: </b> Im Sicherheit-System können Sie unterschiedliche  
	<a href="${pagePrefix}guarantees"><u>Sicherheitstypen</u></a> einstellen. <br>
	<br>
	
	<li><b><a href="${pagePrefix}loan_groups"><u>Darlehensgruppen:</u></a></b> Ansicht 
	und Einrichten von Darlehensgruppen. Darlehensgruppen sind Gruppenmitglieder, die nach 
	Art eines Mikrokredits Darlehen erhalten und zurück zahlen können.<br>
	<br>
	<li><b><a href="${pagePrefix}member_records"><u>Mitgliedseinträge:</u></a></b> Mitgliedseinträge 
	ermöglichen Ihnen, zu definieren, welche Information über Mitglieder gesammelt wird. Diese 
	Berechtigung ermöglicht dem Administrator, einen solchen Eintragstyp einzurichten.<br>
	<br>
	<li><b>Nachrichtenkategorien:</b> ermöglicht dem Administrator, die unterschiedlichen 
	Kategorien für das interne <a href="${pagePrefix}messages"><u>Nachrichten</u></a>-System.<br>
	<br>
	<li><b>Systemzahlungen:</b> Hier können Sie die Berechtigungen für Zahlungen von 
	<a href="${pagePrefix}account_management#standard_accounts"><u>Systemkonten</u></a> 
	zuordnen.
	<ul>
		<li><b>Systemzahlung:</b> Der Administrator kann Systemzahlungen durchführen, die zu den 
		gewählten  <a href="${pagePrefix}account_management#transaction_types">
		<u>Überweisungstypen</u></a> gehören.
		<li><b>Autorisieren:</b> Die Berechtigung, Systemzahlungen zu  <a
			href="${pagePrefix}payments#authorized"><u>autorisieren</u></a>.
		<li><b>Annullieren:</b> Die Berechtigung,  <a
			href="${pagePrefix}payments#scheduled"><u>geplante Zahlungen</u></a>
			annullieren.
		<li><b>Zahlung rückbelasten:</b> Ein Administrator mit dieser Berechtigungen kann eine 
		Zahlung &quot;rückbelasten&quot;, was bedeutet, das in umgekehrter Richtung eine Zahlung mit 
		gleichem Betrag getätigt wird. Hat die Zahlung andere Überweisungen generiert (wie z.B. 
		Gebühren und Darlehen), so verursachen alle Überweisungen umgekehrte Überweisungen.
		Den maximalen Zeitraum den eine Überweisung rückbelastet werden kann können Sie in den
	 <a	href="${pagePrefix}settings#local"><u>Basiseinstellungen</u></a> definieren.
	</ul>
	<br> 
	<br>
	<li><b>Berichte:</b> Jedes Element entspricht einem Element des Hauptmenüs.
	<ul>
		<li><b><a href="${pagePrefix}reports#current_state"><u>Zustandsbericht</u></a></b>
		<li><b><a href="${pagePrefix}reports#member_lists"><u>Mitgliedslisten</u></a></b>
		<li><b>Gesendete SMS Nachrichten:</b> das System kann zu unterschiedlichen Ereignissen 
		SMS-Nachrichten versenden, je nach Konfiguration. Diese Berechtigung erlaubt dem 
		Administrator Zugriff auf die Berichte mit Übersichten der versandten SMS-Nachrichten. 
		<li><b><a href="${pagePrefix}statistics"><u>Statistiken</u></a></b> ermöglicht 
		Zugang zum Statistik-Modul.
	</ul>	
	<br>
	<br>
	<li><b><a href="${pagePrefix}settings#web_services_clients"><u>Web-Services-Clients</u></a></b> 
	Definieren Sie hier die Zugangsebene für die Verbindung zwischen externer Software mit den 
	Cyclos-Webservices.<br>
	<br>
	<li><b>Einstellungen:</b> Dies ermöglicht dem Administrator Zugang zu Menü Einstellungen. 
	<ul>
		<li><b><a href="${pagePrefix}settings#local"><u>Basiseinstellungen verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#alerts"><u>Meldungseinstellungen verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#access"><u>Zugangseinstellungen verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#mail"><u>E-Mail Einstellungen verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#log"><u>Protokolleinstellungen verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#channels"><u>Kommunikationswege verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#web_services_clients"><u>Web-Services-Clients verwalten</u></a></b>
		<li><b><a href="${pagePrefix}settings#import_export"><u>Einstellungen exportieren/importieren</u></a></b>
	</ul>
	<br>
	<br>
	<li><b>Systemstatus:</b> ermöglicht dem Administrator, den Systemstatus anzuschauen. 
	<ul>
		<li><b>Systemstatus anzeigen:</b> wird dies gewählt, so sieht der Administrator das Fenster 
		<a href="${pagePrefix}home#home_status"><u>Systemstatus anzeigen</u></a> auf seiner 
		Startseite. 
		<li><b>Angemeldete Administratoren anzeigen:</b> wählen Sie hier, welche  
		<a href="#admin_groups"><u>Administratorengruppen</u></a> der Administrator im Fenster <a
			href="${pagePrefix}user_management#connected_users"><u>&quot;Angemeldete Benutzer&quot;</u></a> 
    anzeigen kann.
		<li><b>Angemeldete Mitglieder:</b> (funktioniert auf die gleiche Art und Weise). 
		Es gibt ein Kontrollkästchen für alle  <a href="#member_groups"><u>Mitgliedsgruppen</u></a>. 
		Der Administrator kann lediglich die angemeldeten Mitglieder der Gruppen zeigen, für die er 
		eine Berechtigung hat. 
		<li><b>Angemeldete Broker anzeigen:</b> (funktioniert auf die gleiche Art und Weise). 
		Der Administrator kann lediglich die angemeldeten Broker der <a href="#broker_groups">
		<u>Brokergruppen</u></a> anzeigen, für die er eine Berechtigung hat. 
		<li><b>Angemeldete Operatoren anzeigen:</b> (funktioniert auf die gleiche Art und Weise). 
		Zeigt den Operator und das Mitglied, zu dem er gehört. 
	</ul>
	<br>
	<li><b>Administrative Aufträge:</b> Ermöglicht dem Administrator, den Systemstatus 
	anzuschauen. 
	<ul>
		<li><b>System verfügbar setzen:</b> wird dies gewählt, kann der Administrator
		die <a href="${pagePrefix}settings#system_availability"><u>Systemverfügbarkeit</u>
		</a> kontrollieren.
		<li><b>Suchindexe verwalten:</b> wird dies gewählt, kann der Administrator <a
			href="${pagePrefix}settings#search_indexes"><u>Suchindexe verwalten</u></a>
	</ul>	
	<li><b>Themes:</b> Verwalten von <a	href="${pagePrefix}content_management#themes">
	<u>Themes</u></a> unter dem Menü  <a href="${pagePrefix}content_management">
	<u>Content Management</u></a>.<br>
	<br>
	<li><b>Übersetzung:</b> ermöglicht Zugang zum Hauptmenü  <a
		href="${pagePrefix}translation"><u>Übersetzung</u></a>, wo Sie Übersetzungen in Ihre 
		Sprache anzeigen und verwalten können. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_permissions_admin_member"></a>
<h3>Administratorberechtigungen für Mitglieder</h3>
In diesem Fenster können Sie die Berechtigungen für die Mitgliedsfunktionen einer 
Administratorengruppe einstellen. Diese Berechtigungen sind in der Regel für die 
Gruppe &quot;Kontenadministrator&quot; anwendbar. Die Berechtigungsstruktur ist 
recht einfach. Die meisten Funktionen haben Berechtigungen zur &quot;Ansicht&quot; 
und zum &quot;Verwalten&quot;, und in einigen Fällen auch noch zusätzliche, besondere 
Berechtigungen.<br>
Wird &quot;Ansicht&quot; nicht gewählt, erscheint die Funktion nicht im Menü, oder als 
Mitglieds-Aktions-Schaltfläche (gäbe der Administrator die Adresse direkt im Browser ein, 
so zeigte die darunterliegende Berechtigungsstruktur eine Seite mit &quot;Keine 
Berechtigungen&quot;).<br> 
Die Option &quot;Verwalten&quot; berechtigt zum Einrichten, Bearbeiten und Löschen. 
<p>
Im der ersten Auswahl (&quot;Gruppen verwalten&quot;) können Sie einen oder mehrere 
Mitgliedsgruppen wählen. Dies bedeutet, dass der Administrator (dieser Gruppe) nur 
Mitglieder der ausgewählten Gruppen verwalten kann, und auch nur die Information in Zusammenhang 
mit diesen Gruppen zeigen kann. Dies bedeutet, dass alle anderen Informationen im 
Zusammenhang mit Mitgliedern, wie Darlehen, Berichte, Meldungen, angemeldete Benutzer 
etc. nur dann erscheinen, wenn Sie mit den gewählten Gruppen in Zusammenhang stehen.
<br>
Diese Option ermöglicht Ihnen, spezifische Mitgliedsgruppen von spezifischen Kontenadministratoren 
verwalten zu lassen. 
<p>
Die folgenden Berechtigungen können Sie einstellen – bitte verwenden Sie die Links für mehr 
Information. 
<ul>
	<li><b>Zugang:</b> Berechtigungen zu Kontrolle des Zugangs für Mitglieder der 
	Mitgliedsgruppe. Hier finden Sie die folgenden Unterelemente: 
	<ul>
		<li><b>Kennwort ändern:</b> ermöglicht dem Administrator, das Mitgliedskennwort von 
		der Profilseite des Mitglieds zu ändern (unter Mitglieds-Aktionen &quot;Benutzerkennwort 
		verwalten&quot;.<br>
		Bitte beachten Sie: Wurde diese Berechtigung nicht gewählt, kann ein Administrator bei der 
		Mitgliedsregistrierung immer noch ein temporäres Kennwort definieren (bei der ersten Anmeldung 
		wird der Benutzer aufgefordert, das Kennwort zu ändern). Dies ist allerdings nur dann der Fall, 
		wenn die <a href="#edit_member_group"><u>Gruppeneinstellung</u></a> &quot;Kennwort per E-Mail 
		senden&quot; nicht gewählt wurde.<br>
		Wurde die Gruppeneinstellung &quot;Kennwort per E-Mail senden&quot; gewählt, so kann der 
		Administrator das Kennwort nicht definieren. In diesem Fall erhält der Benutzer dann per E-Mail 
		ein temporäres Kennwort, das bei der ersten Anmeldung geändert werden muss.<br>
		Wurden sowohl &quot;Kennwort per E-Mail senden&quot;  als auch &quot;Benutzerkennwort ändern&quot; 
		gewählt, kann der Administrator bei der Registrierung ein dauerhaftes (kein temporäres) Kennwort 
		bestimmen, oder aber den Benutzer auffordern, das Kennwort bei der ersten Anmeldung zu 
		ändern. 
		<li><b>Benutzerkennwort zurücksetzen:</b> ermöglicht dem Administrator das Kennwort des Mitglieds 
		zurück zu setzen, was (je nach Konfiguration) normalerweise bedeutet, dass es automatisch 
		neu generiert und per E-Mail versandt wird. 
		<li><b><a href="${pagePrefix}passwords#transaction_password"><u>Überweisungskennwort</u></a> 
		verwalten:</b> ermöglicht die Verwaltung der besonderen Kennwörter, die für die Überweisungen 
		eingestellt werden können. 
		<li><b>Angemeldetes Mitglied trennen:</b> ermöglicht dem Administrator, ein gerade angemeldetes 
		Mitglied mit sofortiger Wirkung aus dem System abzumelden. 
		<li><b>Angemeldeten Operator trennen:</b> ermöglicht dem Administrator, einen gerade angemeldeten 
		<a href="${pagePrefix}operators"><u>Operator</u></a> mit sofortiger Wirkung aus dem System 
		abzumelden. 
		<li><b>Erlauben gesperrte Mitglieder (durch zu viele Kennwortversuche) anzumelden:</b> 
		Hat ein Mitglied sein Kennwort vergessen, und versucht mehrere Male, sich mit dem falschen Kennwort 
		anzumelden, so wird es zeitweise gesperrt. Ist diese Berechtigung eingestellt, kann der 
		Administrator einem solchen Mitglied sofort wieder den Zugang erlauben. In einem solchen 
		Fall erscheint im Mitgliedsprofil die Schaltfläche &quot;Benutzer darf sich jetzt anmelden&quot;. 
		<li><b>PIN ändern:</b> Ermöglicht dem Administrator, die  <a
			href="${pagePrefix}passwords#pin"><u>PIN-Nummer</u></a>, zu ändern. Dies ist ein numerisches 
			Kennwort für den Zugang zu bestimmten  <a href="${pagePrefix}settings#channels">
			<u>Kommunikationswege</u></a>, wie z.B. Internetshops. 
		<li><b>Zugang Kommunikationswege ändern:</b> Ändern der Zugangsmethoden zu Kommunikationswegen, 
		wie z.B. Internet, Handy, etc. 
		<li><b>PIN entsperren:</b> Entsperren der PIN, falls das Mitglied die maximale Anzahl der 
		Anmeldeversuche überschritten hat. 
	</ul>
	<br>
	<br>
	<li><b>Konten:</b> Berechtigungen in Zusammenhang mit den Berechtigungen der 
	Administratorengruppe, die Mitgliedskonten zu zeigen und zu verwalten. Hier finden Sie die 
	folgenden Unterelemente: 
	<ul>
		<li><b>Informationen anzeigen:</b> Ermöglicht dem Administrator die Kontoinformation 
		zu zeigen (Saldo, Überweisungsübersicht, etc). 
		<li><b>Autorisierte Zahlungen anzeigen:</b> Ermöglicht dem Administrator  <a
		href="${pagePrefix}payments#authorized"><u>Autorisierte Zahlungen</u></a> 
		zu zeigen.
		<li><b>Geplante Zahlungen anzeigen:</b> Ermöglicht dem Administrator <a
		href="${pagePrefix}payments#scheduled"><u>Geplante Zahlungen</u></a> zu zeigen.
		<li><b>Kreditlimit definieren:</b> Ermöglicht dem Administrator, individuelle Kreditlimits 
		einzustellen; diese überschreiben dann die für die Gruppe eingestellten Kreditlimits. 
	</ul>
	<br>
	<br>
	
	<li><b>Inserate:</b> Diese Berechtigungen ermöglichen dem Administrator dieser 
	Administratorengruppe, die Inserate eines Mitglieds zu zeigen und zu verwalten.<br>
	Ebenso gibt es eine Berechtigung für die Funktion  <a
		href="${pagePrefix}advertisements#import_ads"><u>Inserate importieren</u></a>.
	<br>
	<br>
	<li><b>Brokering:</b> Berechtigungen in Zusammenhang mit dem Brokering von Mitgliedern 
	durch	<a href="${pagePrefix}brokering"> <u>Broker</u></a>.
	<ul>
		<li><b>Broker ändern:</b> ermöglicht dem Administrator, den Broker des Mitglieds 
		zu ändern. 
		<li><b>Mitgliederliste anzeigen (als Broker):</b> Ermöglicht dem Administrator, 
		die Mitglieder eines Brokers zu zeigen. 
		<li><b>Darlehendetails in Mitgliederliste anzeigen (drucken):</b> Ermöglicht das
		Anzeigen der Darlehen in der Liste der durch einen Broker verwalteten Mitglieder.
		<li><a href="${pagePrefix}brokering#commission"><u>Kommissionen</u></a><b> verwalten:</b> 
		Verwalten der Kommissionen, die ein Broker erhalten kann. 
	</ul>
	<br>
	<br>
	<li><b>Massenaktionen:</b> Mit <a href="${pagePrefix}user_management#bulk_actions"><u>
	Massenaktionen</u></a> können Sie eine bestimmte Aktion für eine ganze Reihe 
	von Mitgliedern durchführen. 
	<ul>
		<li><b>Gruppe ändern:</b> Ändern der Gruppe für eine Reihe von Mitgliedern. 
		<li><b>Broker ändern:</b> Ändern der Gruppe für eine Reihe von Mitgliedern. Dies möchten 
		Sie möglicherweise tun, wenn ein Broker seine Tätigkeit niederlegt und seine Mitglieder 
		einen anderen Broker brauchen. 
	</ul>
	<br>
	<br>
	<li><b>Dokumente:</b> Ermöglicht dem Administrator, <a
		href="${pagePrefix}documents"> <u>Dokumente</u></a> zu verwalten und zeigen. 
		Hier finden Sie die folgenden Unterelemente
	<ul>
		<li><b>Dokument anzeigen:</b> Hier können Sie wählen, welche Dokumente der Administrator 
		anzeigen kann. Sind keine Dokumente verfügbar, ist das Auswahlfeld leer.
		<li><b>Dynamische <a href="${pagePrefix}documents"><u>Dokumente</u></a> anzeigen</b>
		<li><b>Statische <a href="${pagePrefix}documents"><u>Dokumente</u></a> anzeigen</b>
		<li><b>Mitgliedsdokumente<a href="${pagePrefix}documents#member_document"><u></u></a> anzeigen</b>
	</ul>
	<br>
	<br>
	<li><b>Mitgliedsgruppen:</b>
	<ul>
		<li><b>Ansicht:</b> Ist dies eingestellt, kann der Administrator die  
		<a href="#manage_groups"><u>Übersicht</u></a> der Mitgliedsgruppen zeigen. 
		<li><b>Kontoeinstellungen verwalten:</b> Ermöglicht dem Administrator die  <a
			href="#manage_group_accounts"><u>Kontoeinstellungen zu verwalten</u></a>. Ist dies 
			NICHT eingestellt, kann der Administrator die Einstellungen zwar zeigen (abhängig 
			vom vorherigen Berechtigungselement), aber keine Veränderungen vornehmen.  
		<li><b>Benutzerdefinierte Dateien verwalten:</b> Ermöglicht dem Administrator,  <a
			href="${pagePrefix}content_management#customized_files"><u>benutzerdefinierte 
			Dateien</u></a> für diese Gruppe zu verwalten. 
	</ul>
	<br>
	<br>
	<li><b>Sicherheiten:</b> Verschiedene Berechtigungen in Zusammenhang mit dem  <a
		href="${pagePrefix}guarantees"><u>Sicherheit</u></a>-System. Hier finden
		 Sie die folgenden Unterelemente: 
	<ul>
		<%-- TO DO --%>
		<li><b>Zahlungsverpflichtungen anzeigen:</b>
		<li><b>Zertifizierungen anzeigen:</b>
		<li><b>Sicherheiten anzeigen:</b>
		<li><b>Verarbeiten von Darlehens-Sicherheiten:</b>
		<li><b>Sicherheiten registrieren:</b>
		<li><b>Zahlungsverpflichtungen widerrufen:</b>
		<li><b>Zertifizierungen widerrufen:</b>
		<li><b>Sicherheiten ablehnen / widerrufen:</b>
		<li><b>Sicherheiten akzeptieren:</b>
	</ul>
	<br>
	<br>
	<li><b>Mitgliedsrechnungen:</b> Verschiedene Berechtigungen, die dem Administrator 
	Zugang zu Mitglieds-<a href="${pagePrefix}invoices"><u>Rechnungen</u></a> geben. 
	All diese Elemente sind recht einfach nachzuvollziehen, wir werden sie also hier 
	nicht Punkt für Punkt behandeln. <br>
	<br>
	<li><b><a href="${pagePrefix}loan_groups"><u>Darlehensgruppe Mitgliedschaft:</u></a></b> 
	Hier können Sie definieren, ob ein Administrator einer Darlehensgruppe Mitglieder hinzufügen 
	oder aus ihr entfernen darf, oder ob er die Darlehensgruppen nur zeigen kann.<br>
	<br>
	<li><b><a href="${pagePrefix}loans"><u>Darlehen:</u></a></b> Verschiedene Einstellungen, 
	die Zugang zu Mitgliedsdarlehen geben. Die folgenden Unterpunkte gibt es: 
	<ul>
		<li><b>Mitgliedsdarlehen anzeigen:</b> Normale Darlehen der Mitglieder zeigen. 
		<li><b>Autorisierte Darlehen anzeigen:</b> Für einige Darlehen wird eine besondere Berechtigung benötigt. 
		<li><b>Darlehen bewilligen:</b> Im Auswahlfeld können Sie spezifizieren, welche Darlehenstypen 
		der Administrator den Mitgliedern bewilligen kann. 
		<li><b>Darlehensbewilligung rückdatieren:</b> Für bestimmte Gelegenheiten, wenn das Darlehendatum 
		nicht das derzeitige Datum sein kann.
		<li><b><a href="${pagePrefix}loans#discard"><u>Darlehen erledigen:</u></a> </b> 
		ermöglicht es, ein Darlehen zu &quot;annullieren&quot; indem der Restbetrag auf 0 
		gestellt wird. 
		<li><b>Darlehen zurückzahlen:</b> Ermöglicht dem Administrator, für das Mitglied ein Darlehen 
		zurück zu zahlen. 
		<li><b>Darlehenrückzahlung rückdatieren:</b> Wie oben, diesmal ist es allerdings das 
		Rückzahlungsdatum, das der Administrator auf ein Datum in der Vergangenheit datieren kann. 
		<li><b>Status für abgelaufenen Darlehen verwalten:</b> Ermöglicht Ihnen, einem abgelaufenen 
		Darlehen einen <a href="${pagePrefix}loans#status"><u>besonderen Status/Markierung</u></a> 
		zu geben.
	</ul>
	<br>
	<br>
	<li><b>Nachrichten:</b> Ermöglicht dem Administrator Zugang zum  <a
		href="${pagePrefix}messages"><u>Nachrichten</u></a>-System von Cyclos.
	<ul>
		<li><b>Ansicht:</b> Beinhaltet ein Auswahlfeld, in dem Sie wählen können, welche Typen von
		 Nachrichtenkategorien der Administrator zeigen kann. Neue Nachrichtentypen richten 
		 Sie über <a href="${pagePrefix}messages#categories"><u>Nachrichtenkategorien</u></a> ein.
		<li><b>An Mitglied senden:</b> Ermöglicht dem Administrator, Nachrichten an 
		einzelne Mitglieder zu versenden. 
		<li><b>An Gruppe senden:</b> Ermöglicht dem Administrator, Nachrichten an alle Mitglieder 
		einer bestimmten Gruppe zu versenden.
		<li><b>Verwalten:</b> Ermöglicht das Verwalten von Nachrichten. Dies ermöglicht zum Beispiel 
		die Suche nach alten Nachrichten und das Einrichten von Nachrichtenkategorien. 
	</ul>
	<br>
	<br>
	<li><b>Mitgliedszahlungen:</b> Dies ist eine Reihe von Berechtigungen in Zusammenhang mit  <a
		href="${pagePrefix}payments"><u>Zahlungen</u></a>. Unterpunkte sind:
	<ul>
		<li><b>Zahlung System an Mitglied:</b> Wählen Sie hier die Systemzahlungen, die ein 
		Administrator verwenden kann, um eine Zahlung von einem Systemkonto an ein Mitglied 
		zu tätigen.
		<li><b>Mitgliedszahlung im Menü anzeigen:</b> Ermöglicht dem Administrator, Zahlungen 
		System-an-Mitglied direkt über das Hauptmenü zu tätigen. 
		<li><b>Mitgliedszahlung rückdatieren:</b> Ermöglicht dem Administrator, Zahlungen 
		System-an-Mitglied tätigen, und deren Datum &quot;rückdatieren&quot;, es kann ein 
		bereits vergangenes Datum als Zahlungsdatum eingestellt werden. 
		<li><b>Zahlung Mitglied an Mitglied:</b> Wählen Sie hier die Zahlungstypen, die der 
		Administrator ausführen kann, so als sei er ein Mitglied, das eine Zahlung an ein 
		anderes Mitglied leistet. 
		<li><b>Mitglied Eigenzahlung:</b> Dies sind Überweisungen von einem Konto eines Mitglieds 
		auf ein anderes Konto des gleichen Mitglieds. Der Administrator kann diese Zahlungen so 
		ausführen, als sei er das Mitglied. 
		<li><b>Zahlung Mitglied an System:</b> Der Administrator kann eine Zahlung vom Mitgliedskonto 
		auf ein Systemkonto ausführen, so als sei er das Mitglied. Wählen Sie hier die Zahlungstypen, 
		für die Sie das zulassen möchten. 
		<li><b><a href="${pagePrefix}payments#authorized"><u>Zahlungen autorisierten</u>:</a></b> 
		Dies sind Zahlungen, für die besondere Autorisierungen notwendig sind; hier können Sie 
		einstellen, ob der Administrator eine Zahlung so autorisieren kann, als sei er das Mitglied. 
		<li><b>Autorisierte Zahlungen als Mitglied annullieren:</b>
		<li><b><a href="${pagePrefix}payments#scheduled"><u>Geplante Zahlung</u></a> als Mitglied 
		annullieren:</b>
		Damit diese Einstellung funktionieren kann, muss die Konfiguration des Überweisungstyps
		auch die Annulierung von geplanten Zahlungen zulassen.
		<li><b>Geplante Zahlung als Mitglied sperren:</b> Hiermit können Sie eine geplante Zahlung 
		sperren. Der Unterschied zwischen Sperren und Annullieren ist der, dass eine Sperrung wieder 
		aufgehoben werden kann. Annullierung ist endgültig.<br> 
		Damit diese Einstellung funktionieren kann, muss die Konfiguration des Überweisungstyps
		auch die Sperrung von geplanten Zahlungen zulassen.
		<li><b><a href="${pagePrefix}payments#charge_back"><u>Zahlung rückbelasten:</u></a></b> 
		Ermöglicht dem Administrator, eine Zahlung für ein Mitglied rückgängig zu machen. 
	</ul>
	<br>
	<br>
	<li><b><a href="${pagePrefix}member_records"><u>Mitgliedseinträge:</u></a></b>
	Ermöglicht dem Administrator, die Mitgliedseinträge zu verwalten – und zu definieren, 
	welche Information über Mitglieder gesammelt wird. Unterpunkte erklären sich von 
	selbst.<br>
	<br>
	<li><b><a href="${pagePrefix}references"><u>Referenzen:</u></a></b> Ermöglicht dem 
	Administrator, Referenzen zu verwalten und zu zeigen. Referenzen sind das System, mit 
	dem Mitglieder sich gegenseitig gute oder schlechte Bewertungen geben können.<br>
	<br>
	<li><b><a href="${pagePrefix}reports#member_activities"><u>Mitgliedsberichte:</u></a></b> 
	Dies sind Berichte zu den Aktivitäten einzelner Mitglieder, zugänglich über die 
	Aktions-Schaltflächen unterhalb des Mitgliedsprofils. Unterpunkte erklären sich von 
	selbst.<br>
	<br>
	<li><b><a href="${pagePrefix}reports#sms_log"><u>SMS-Protokolle:</u></a></b>
	Ermöglicht Zugang zu den Protokollen der an dieses Mitglied gesandten SMS-Nachrichten. 
	Das System kann für das Versenden von SMS-Nachrichten zu unterschiedlichen 
	Ereignissen konfiguriert werden.<br>
	<br>
	
	<li><b><a href="${pagePrefix}transaction_feedback"><u>Feedbacks zu Geschäftsvorgängen:</u></a></b> 
	Dies sind Feedbacks anderer Mitglieder zu bestimmten Geschäftsvorgängen. Diese Berechtigung 
	ermöglicht es, Feedbacks zu Geschäftsvorgängen (durch einen Administrator) zu zeigen und zu ändern. 
	Feedbacks zu Geschäftsvorgängen aktivieren Sie in der Konfiguration des
  <a href="${pagePrefix}account_management#transaction_type_details"><u>Überweisungstyps</u></a>.<br>
	<br>
	<li><b>Mitglieder:</b> Verschiedene Berechtigungen für Aktivitäten des Administrators im 
	Zusammenhang mit Mitgliedern. Unterpunkte: 
	<ul>
		<li><b>Ansicht:</b> Ist dies nicht gewählt, kann der Administrator keine Mitglieder zeigen. 
		<li><b>Registrieren:</b> Wird dies gewählt, kann der Administrator ein neues Mitglied über die 
		Seite &quot;Mitglieder suchen&quot; registrieren. 
		<li><b>Profil ändern:</b> Wird dies gewählt, kann der Administrator Änderungen im 
		<a href="${pagePrefix}profiles"><u>Profil</u></a> des Mitglieds vornehmen.
		<li><b>Definitiv entfernen:</b> Der Administrator kann Mitglieder dauerhaft aus 
		der Datenbank entfernen. Dies kann nur dann getan werden, wenn das Mitglied nie zu einer 
		Gruppe gehört hat, die Konten unterhielt (nach der Aktivierung kann ein Mitglied immer 
		in die Gruppe <a href="#removed_members"><u>Gelöschte Mitglieder</u></a> verschoben werden).			
		<li><b>Gruppe ändern:</b> Ermöglicht dem Administrator, die Gruppe des Mitglieds zu ändern. 
		<li><b>Import:</b> Ermöglicht dem Administrator, Mitgliedslisten in Cyclos zu importieren 
		(normalerweise Migration aus anderen Systemen).<br> 
		Mehr Information dazu finden Sie <a	href="${pagePrefix}user_management#import_members"><u>hier</u></a>.
		<li><b>Interessierte Mitglieder einladen:</b> Der Administrator kann die Interessenten zeigen, 
		die sich zwar bereits registriert haben, aber ihre Registrierung noch nicht per E-Mail bestätigt 
		haben.<br> 
		Mehr Information dazu finden Sie <a	href="${pagePrefix}user_management#search_pending_member">
		<u>hier</u></a>.
	</ul>
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_permissions_admin_admin"></a>
<h3>Verwaltungsberechtigungen Administratoren</h3>
In diesem Fenster können Sie die Berechtigungen für die Administratorenfunktionen einer 
Administratorengruppe einstellen. Das bedeutet, dass sie die Zugangsebene definieren, 
die die Administratoren im System haben: Mitglieder dieser spezifizierten Administratorengruppen 
erhalten die von Ihnen angekreuzten Berechtigungen. Es ist üblich, eine begrenzte Anzahl 
(oder auch nur einen) Administratoren mit dieser Art Berechtigung auszustatten, da es 
sich hier um Berechtigungen auf höchster Ebene handelt. 
<br>
Das Berechtigungsstruktur ist recht einfach. Die meisten Funktionen haben Berechtigungen zur 
&quot;Ansicht&quot; und zum &quot;Verwalten&quot;, und in einigen Fällen auch noch zusätzliche, 
besondere Berechtigungen. 
<br>
Wird &quot;Ansicht&quot; nicht gewählt, erscheint die Funktion nicht im Menü, oder als 
Mitglieds-Aktions-Schaltfläche (gäbe der Administrator die Adresse direkt im Browser ein, so zeigte 
die darunterliegende Berechtigungsstruktur eine Seite mit &quot;Keine Berechtigungen&quot;).<br> 
Die Option &quot;Verwalten&quot; berechtigt zum Einrichten, Bearbeiten und Löschen. 
<br>
<p>
Ist die Funktion mit bestimmten Berechtigungen verbunden ( zusätzlich zu &quot;Verwalten&quot; und 
&quot;Ansicht&quot;), so ist die Berechtigung mit einem Namen bezeichnet, der ihren Typ kennzeichnet 
(z.B. &quot;Angemeldetes Mitglied abmelden&quot;).<br>
<br>
Die Folgenden sind verfügbar; wir erwähnen dabei nur diejenigen, die einer Erklärung bedürfen. 
<ul>
	<li><b>Zugang:</b>
	<ul>
		<li><b>Administrator abmelden:</b> Ermöglicht es, einen anderen Administrator mit sofortiger 
		Wirkung aus dem System abzumelden. 
		<li><b>Gesperrten Administrator (durch zu viele Kennwortversuche) erlauben, sich wieder anzumelden:</b>
		Wird ein Administrator gesperrt weil er sein Kennwort vergessen hat, können Sie im ermöglichen, 
		sich wieder anzumelden. 
	</ul>
	<br>
	<br>
	<li><b>Administratoreinträge:</b> Diese sind wie <a
		href="${pagePrefix}member_records"><u>Mitgiedseinträge</u></a>, ermöglichen Ihnen allerdings, 
		Informationen für Administratoren einzutragen. Unterpunkte erklären sich von selbst.<br>
	<br>
	<li><b>Administratoren:</b> Dies sind Berechtigungen in Zusammenhang mit anderen Administratoren,
	 wie deren Registrierung, oder deren Verschieben in eine andere Gruppe. Es gibt weniger 
	 Administratoren- als Mitgliedsfunktionen. Administratoren haben keine Konten, können aber nur 
	 bis zu einer bestimmten Ebene Zugang zu Systemkonten haben.<br>
	 Die Elemente erklären sich von selbst. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_permissions_member"></a>
<h3>Mitgliedsberechtigungen für Mitgliedsgruppe</h3>
Die Mitglieder, die dieser Gruppe angehören, erhalten diese Berechtigungen. 
<ul>
	<li><b>Zugang:</b> PIN entsperren, falls das Mitglied die maximale Anzahl der 
	Anmeldeversuche überschritten hat.<br>
	<br>
	<li><b>Konto:</b> Ein Mitglied hat immer Zugang zum eigenen Konto, es gibt also 
	keine Berechtigung für die Ansicht oder das Verwalten des eigenen Kontos. In diesem Abschnitt 
	können Sie nur einstellen, ob es dem Mitglied möglich ist folgende Punke anzusehen: 
	<ul>
		<li><b><a href="${pagePrefix}payments#authorized"><u>Autorisierte Zahlungen</u></a></b>
		<li><b><a href="${pagePrefix}payments#scheduled"><u>Geplante Zahlungen</u></a></b>
	</ul>
	<br>
	<br>
	<li><b>Inserate:</b><br>
	<ul>
		<li><b>Veröffentlichen:</b> Wenn Sie &quot;Veröffentlichen&quot; wählen, kann 
		ein Mitglied Inserate veröffentlichen, und das Menü-Element &quot;Persönlich – Inserate&quot; 
		erscheint im Mitgliedsmenü.<br>
		<br>
		<li><b>Ansicht:</b> Werden im Auswahlfeld &quot;Ansicht&quot; keine Gruppen gewählt, sind 
		die Inserate-Funktionen für die Mitglieder dieser Gruppe nicht verfügbar. Ebensowenig 
		erscheinen sie dann im Suchmenü oder auf der Seite  <a
			href="${pagePrefix}profiles#actions_for_member"><u> &quot;Aktionen&quot;</u></a> 
		unter dem Mitgliedsprofil.<br>
		Werden eine oder mehrere Gruppen gewählt, zeigt die Funktion  <a
			href="${pagePrefix}advertisements#advertisement_search"><u> &quot;Suche - Inserate&quot;</u></a> 
		lediglich die Ergebnisse für die Mitglieder der gewählten Gruppen.<br>
		Normalerweise wählen Sie hier alle Gruppen. Falls Sie Gruppen haben, die zwar innerhalb des 
		gleichen Systems, aber vollkommen separat operieren müssen, könnten Sie die Sichtbarkeit 
		eingrenzen, indem Sie nur bestimmte Gruppen wählen. Ein Beispiel hierfür wäre eine Gewerbegruppe, 
		der Sie keine Konsumenten-Inserate dieser Gruppe zeigen möchten. 
	</ul>
	<br>
	<br>
	<li><b>Kommissionen:</b> Wird hier &quot;Ansicht&quot; gewählt, kann das Mitglied die  
	<a href="${pagePrefix}brokering#commission"><u>Kommissionen</u></a> für seinen eigenen Broker 
	anschauen.<br>
	<br>
	<li><b>Dokumente:</b> Mit dieser Option können Sie festlegen, welche <a
		href="${pagePrefix}documents"><u>Dokumente</u></a> im Mitgliedsmenü &quot;Persönlich – Dokumente&quot; 
		erscheinen. Wird hier kein Dokument gewählt, so erscheint dieses Menüelement nicht 
		(für diese Mitgliedsgruppe).<br>
	<br>
	<li><b>Sicherheiten:</b> Dies ist Teil des <a href="${pagePrefix}guarantees">
	<u>Sicherheit</u></a>-Systems von Cyclos, in dem jeder Saldo durch einen Deckungsbetrag 
	garantiert ist. Sie können die folgenden Berechtigungen wählen: 
	<ul>
		<li><b>Mit Zahlungsverpflichtung von Gruppen kaufen:</b> Hier wählen Sie, 
		von welcher Gruppe das Mitglied berechtigt ist, mit <a
			href="${pagePrefix}guarantees"><u>Zahlungsverpflichtungen</u></a> zu kaufen.
		<li><b>Ausgabezertifizierungen an:</b> <%-- ?? LATER DOOR HUGO ?? --%>
		<li><b>Sicherheiten erstellen (als Aussteller): </b> <%-- ?? LATER DOOR HUGO ?? --%>
		<li><b>Mit Zahlungsverpflichtung an Gruppen verkaufen:</b> <%-- ?? LATER DOOR HUGO ?? --%>
	</ul>
	<br>
	<br>
	<li><b>Rechnungen:</b> In diesem Abschnitt können Sie definieren, ob ein Mitglied  <a
		href="${pagePrefix}invoices"><u>Rechnungen</u></a> an andere Mitglieder schicken kann, 
		entweder von einem Benutzerprofil aus, oder direkt über das Kontomenü. Wird 
		&quot;An System senden&quot; gewählt, so kann ein Mitglied vom &quot;Kontomenü&quot; aus 
		Rechnungen an Systemkonten schicken.<br>
	<br>
	<li><b>Darlehen:</b> In diesem Abschnitt können Sie die Berechtigungen für <a
		href="${pagePrefix}loans"><u>Darlehen</u></a> für Mitglieder einstellen.
	<ul>
		<li><b>Ansicht:</b> Wird diese Option gewählt, können die Mitglieder der Gruppe deren 
		Darlehen ansehen. Wird &quot;Ansicht&quot; nicht gewählt, erscheinen die Menüelemente nicht. 
		<li><b>Zurückzahlen:</b> Wählen dieser Option ermöglicht dem Mitglied, 
		Darlehensrückzahlungen zu tätigen. 
	</ul>
	<br>
	<br>
	<li><b>Nachrichten:</b> In diesem Abschnitt können Sie definieren, in welchem Umfang das 
	Mitglied das <a href="${pagePrefix}messages"><u>Nachrichten</u></a>-System von Cyclos 
	benutzen kann. Damit im Auswahlfeld &quot;An die Administration senden&quot; mehr als eine 
	Kategorie erscheint, müssen Sie diese Kategorien zuerst <a
		href="${pagePrefix}messages#categories"><u>definieren</u></a>.<br>
	Anmerkung: Damit eine Nachricht auch für den Administrator erscheint, benötigt die 
	Administratorengruppe ebenfalls die Berechtigungen.<br>
	<br>
	<li><b>Operatoren:</b> Hier können Sie definieren, ob Mitglieder das <a
		href="${pagePrefix}operators"><u>Operatoren</u></a>-System von Cyclos nutzen können; 
		Sie können hier also eine Art Untermitgliedschaften für ein Konto definieren. Es gibt 
		nur ein Kontrollkästchen, um dies zu wählen oder abzuwählen.<br>
	<br>
	<li><b>Zahlungen:</b> Hier können Sie spezifizieren, welche <a
		href="${pagePrefix}account_management#transaction_type_details"><u>Zahlungstypen</u></a> 
		für diese Mitgliedsgruppe zulässig sind. Meist werden Sie hier nur einen oder wenige 
		Typen auswählen. Die Verfügbarkeit der Typen hängt natürlich davon ab, wie viele 
		Zahlungstypen Sie zuvor definiert haben. 
	<ul>
		<li><b>Eigenzahlung:</b> Wird dies gewählt, kann das Mitglied Zahlungen zwischen seinen 
		eigenen Konten tätigen. Im Auswahlfeld können Sie die möglichen Überweisungstypen 
		spezifizieren. Diese Option macht nur dann Sinn, wenn Sie für diese Gruppe mehr als 
		ein Mitgliedskonto haben. 
		<li><b>Mitgliedszahlungen:</b> Hier können Sie wählen, welche Zahlungstypen das Mitglied 
		für Zahlungen an ein anderes Mitglied verwenden kann. 
		<li><b>Mitgliedszahlung vom Menü:</b> Wird diese Option gewählt, können die 
		Mitglieder-Zahlungen an andere Mitglieder direkt über das Kontomenü vornehmen.
		<li><b>Systemzahlungen:</b> Hier können Sie spezifizieren, welche Zahlungstypen das Mitglied 
		für Zahlungen an Systemkonten verwenden kann. Wird diese Option nicht gewählt, erscheint 
		das Menü-Element &quot;Systemzahlung&quot; nicht. 

		<li><b>Externe Zahlungstickets generieren:</b><br>
		Hier geht es um das Ticket-System das von Mitgliedsläden verwendet wird. Es wird 
		meist verwendet von Internetshops, die Cyclos als externes Zahlungssystem verwenden möchten. 
		Wird diese Option gewählt, kann der (Mitglieds)Laden Tickets generieren. Das Ticket-System 
		ist für die Benutzer recht transparent. Es bietet eine Struktur, innerhalb derer Internetshops 
		Konsumenten und Zahlungsdaten authentifizieren und validieren können, ohne aber Zugang zu den 
		Anmeldedaten der Konsumenten zu erhalten. Die technischen Details finden Sie auf der 
		Cyclos-Wiki-Seite (webservices – tickets). 

		<li><b>Zahlungen autorisieren:</b> Ermöglicht dem Mitglied, eine Zahlung zu 
		<a href="${pagePrefix}payments#authorized"><u>autorisieren</u></a>. Als Zahlungsempfänger
		oder gegebenenfalls durch den Zahler, nachdem der Zahlungsempfänger autorisiert hat. 
		<br>
		Die Konfiguration für die Autorisierungen kann in der 
		Konfiguration der <a
			href="${pagePrefix}account_management#edit_authorization_level"><u>
		Überweisungstypen</u></a> gefunden werden.<br>
		Anmerkung: Die Funktionsweise ist ähnlich der von Rechnungen, daher 
		sollte dies auch nicht mit Rechnungen kombiniert werden. 
		
		<li><b>Anstehende Zahlungsautorisierung annullieren:</b> Werden <a
		href="${pagePrefix}payments#authorized"><u>autorisierte Zahlungen</u></a>
		verwendet, ermöglicht dies den Mitgliedern, autorisierte Zahlungen nach deren Einrichtung, aber 
		vor deren Autorisierung zu annullieren. 

		<li><b>Geplante Zahlung annullieren:</b> Werden <a
		href="${pagePrefix}payments#scheduled"><u>Geplante Zahlungen</u></a> verwendet, ermöglicht dies 
		den Mitgliedern, ihre geplanten Zahlungen vor deren Beginndatum zu annullieren.<br>
		Damit diese Einstellung funktionieren kann, muss die Konfiguration des Überweisungstpys 
		auch die Annullierung von geplanten Zahlungen zulassen. 
		<li><b>Geplante Zahlung sperren:</b> gestattet dem Mitglied, seine geplante Zahlung 
		vorübergehend zu sperren.<br>
		Damit diese Einstellung funktionieren kann, muss die Konfiguration des Überweisungstyps
		auch die Sperrung von geplanten Zahlungen zulassen. 
		<li><b>Zahlungen über andere Kommunikationswege anfordern:</b> Ist dieses Kästchen ausgefüllt, 
		kann das Mitglied &quot;<a href="${pagePrefix}payments#request"><u>
		Zahlungsanforderungen</u></a>&quot; (Rechnungen) über andere  <a href="${pagePrefix}settings#channels">
		<u>Kommunikationswege</u></a> versenden; Die Auswahl dieser Kommunikationswege treffen 
		Sie bitte über das Auswahlfeld. Zur Zeit ist dafür SMS verfügbar, in der Zukunft werden 
		allerdings auch noch andere Kommunikationswege möglich sein. 
	</ul>
	<br>
	<li><b>Benachrichtigungen:</b> Ermöglicht den Mitgliedern einer Gruppe Zugang über das Hauptmenü 
	zu den Benachrichtigungen. In diesem Menü gibt es nur zwei Elemente, daher diese Berechtigungen: 
	<ul>
		<li><b>Benachrichtigungsoptionen verwalten</b> ermöglicht Mitgliedern, ihre <a
			href="${pagePrefix}notifications"><u>Benachrichtigungen</u></a> zu verwalten.
		<li><b>Inserat-Beobachter verwalten</b> ermöglicht Mitgliedern, ihre <a
			href="${pagePrefix}ads_interest"><u>Inserat-Beobachter</u></a> zu verwalten.
	</ul>
	<br>
	<li><b>Mitgliedsprofil:</b> Hier können Sie festlegen, welche Mitglieder in der 
	<a href="${pagePrefix}user_management#search_member_by_member"><u>
	&quot;Mitgliedssuche&quot;</u></a> dieser Mitgliedsgruppe erscheinen. Hier werden normalerweise 
	alle Gruppen gewählt (ausser gelöschten oder gesperrten Gruppen). Hier wählen Sie nur dann bestimmte 
	Gruppen zum Anzeigen aus, wenn Sie in Ihrem System unabhängig voneinander arbeitende Gruppen haben 
	möchten. Zum Beispiel eine Konsumentengruppe und eine Gewerbegruppe, die sich gegenseitig nicht 
	sehen können. Wenn Sie eine bestimmte Gruppe zum Anschauen verwenden, müssen Sie die gleichen 
	Berechtigungen wie für die Anzeige-Berechtigungen in Inserate einstellen.<br>
	<br>
	<li><b>Referenzen:</b> Die ermöglicht den Mitgliedern,  <a
		href="${pagePrefix}references"><u>Referenzen</u></a> zu zeigen, oder sie anderen Mitgliedern zu 
		erteilen. Wenn Sie die Funktion &quot;Referenzen&quot; gar nicht verwenden möchten (für eine 
		oder für mehrere Gruppen), lassen Sie die Option &quot;Ansicht&quot; einfach leer. In diesem Fall werden 
		das Menü &quot;Referenzen&quot; und andere Schaltflächen dazu nicht erscheinen.<br>
	<br>
	<li><b>Mitgliedsberichte:</b> Wurde &quot;Ansicht&quot; gewählt, kann die Mitgliedsgruppe 
	die  <a href="${pagePrefix}reports#member_reports"><u>
	Berichte</u></a> anderer Mitglieder zeigen. Wenn Sie unter &quot;Kontoinformationen anzeigen&quot; 
	einen Überweisungstyp wählen, kann das Mitglied auch Kontoinformationen (Salden) anderer Mitglieder 
	in diesen Berichten sehen.<br>
	<br>
	<li><b><a href="${pagePrefix}reports#sms_log"><u>SMS-Protokolle:</u></a></b> Ermöglicht dem 
	Mitglied Zugang zu den Protokollen der an es selbst gesandten SMS-Nachrichten. Das System 
	kann für das Versenden von SMS-Nachrichten zu unterschiedlichen Ereignissen konfiguriert 
	werden.<br>
	<br>
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_permissions_broker"></a>
<h3>Broker-Berechtigungen</h3>
Hier handelt es sich um die Berechtigungen, die ein Broker zur Ausübung seiner 
<a href="${pagePrefix}brokering"> <u>Brokering</u></a>-Funktionen braucht. Dies 
bedeutet, dass Sie den Typ der Funktionen definieren können, den Broker dieser 
Gruppe in bezug auf ihre Mitglieder ausführen können. Die Berechtigungsstruktur 
ist recht einfach. Die meisten Funktionen haben Berechtigungen zur &quot;Ansicht&quot; 
und zum &quot;Verwalten&quot;, und in einigen Fällen auch noch zusätzliche, besondere 
Berechtigungen.<br> 
Wird &quot;Ansicht&quot; nicht gewählt, erscheint die Funktion nicht im Menü, oder 
als Mitglieds-Aktions-Schaltfläche (gäbe der Administrator die Adrese direkt im Browser 
ein, so zeigte die darunterliegende Berechtigungsstruktur eine Seite mit &quot;Keine 
Berechtigungen&quot;).<br> 
Die Option &quot;Verwalten&quot; berechtigt zum Einrichten, Bearbeiten und Löschen.<br> 
<br>
Sie können festlegen, dass der Broker für die von ihm gebrokerten Mitglieder Zugang zu 
den folgenden Funktionen hat: 
<ul>
	<li><b>Konto:</b>
	<ul>
		<li><b>Kontoinformationen anzeigen:</b> Der Broker kann die Kontoinformationen 
		der von ihm gebrokerten Mitglieder zeigen. 
		<li><b><a href="${pagePrefix}payments#authorized"><u>Autorisierte Zahlungen anzeigen</u></a></b> 
		Falls gewählt, ermöglicht dies dem Broker, die autorisierten Zahlungen seiner Mitglieder zu zeigen. 
		<li><b><a href="${pagePrefix}payments#scheduled"><u>Geplante Zahlungen anzeigen</u></a></b>
	</ul>
	<br>
	<br>
	<li><b>Inserate:</b> Berechtigt den Broker, die Inserate der von ihm gebrokerten Mitglieder 
	zu zeigen oder zu verwalten.<br>
	<br>
	
	<li><b><a href="${pagePrefix}documents">Dokumente:</a></b>
	<ul>	
	<li><b>Ansicht:</b> Hier können Sie wählen, welche systemweiten Dokumente der Broker 
	anzeigen kann. 
	<li><b>Individuelle Mitgliedsdokumente anzeigen:</b> Wird dies gewählt, kann der Broker <u>
	<a href="${pagePrefix}documents#member_document">individuelle Dokumente</a></u> 
	des Mitglieds anschauen.
	<li><b>Individuelle Mitlgiedsdokumente verwalten:</b> Wie oben, aber mit Verwaltungsrechten. 
	</ul>
	<br>
	<li><b>Rechnungen:</b> Die Bedeutung der einzelnen Elemente hier erklärt sich von selbst.<br>
	<br>
	<li><b><a href="${pagePrefix}loan_groups"><u>Darlehensgruppen</u></a></b> <br>
	Menüelement Darlehensgruppen anzeigen.
	<br>
	<br>
	<li><b><a href="${pagePrefix}loans"><u>Darlehen</u></a></b><br>
	Menüelement Darlehen zeigen 
	<br>
	<br>
	<li><b>Zugang:</b> Dies ermöglicht dem Broker, Zugang für das Mitglied zu kontrollieren. 
	Hier finden Sie die folgenden Unterelemente: 
	<ul>
		<li><b>Benutzerkennwort ändern:</b> Ermöglicht dem Broker, das Mitgliedskennwort von der 
		Profilseite des Mitglieds zu ändern.<br>
		Bitte beachten Sie: Wurde diese Berechtigung nicht gewählt, kann ein Broker bei der 
		Mitgliedsregistrierung immer noch ein temporäres Kennwort definieren (bei der ersten Anmeldung 
		wird der Benutzer aufgefordert, das Kennwort zu ändern). Dies ist allerdings nur dann der Fall, 
		wenn die Gruppeneinstellung &quot;Kennwort per E-Mail senden&quot; nicht gewählt wurde.<br>
		Wurde die Gruppeneinstellung &quot;Kennwort per E-Mail senden&quot; gewählt, so kann der Broker 
		das Kennwort nicht definieren (das Mitglied erhält dann per E-Mail ein temporäres Kennwort, das 
		bei der ersten Anmeldung geändert werden muss).<br>
		Wurden sowohl &quot;Kennwort per E-Mail senden&quot; als auch &quot;Benutzerkennwort ändern&quot; 
		gewählt, kann der Broker bei der Registrierung ein dauerhaftes (kein temporäres) Kennwort 
		bestimmen, oder aber den Benutzer auffordern, das Kennwort bei der ersten Anmeldung zu ändern.
		
		</li><b>Benutzerkennwort zurücksetzen:</b> Ermöglicht dem Broker, das Kennwort des Mitglieds 
		zurück zu setzen, was (je nach Konfiguration) normalerweise bedeutet, dass es automatisch 
		neu generiert und per E-Mail versandt wird. 
		<li><b><a href="${pagePrefix}passwords#transaction_password"><u>Überweisungskennwort verwalten:</u></a></b> 
		Ermöglicht die Verwaltung der besonderen Kennwörter, die für die Überweisungen eingestellt 
		werden können. 
		<li><b>PIN ändern:</b> Ermöglicht dem Broker, die  <a 
		href="${pagePrefix}passwords#pin"><u>PIN-Nummer</u></a> zu ändern. Dies ist ein numerisches 
		Kennwort für den Zugang zu bestimmten <a href="${pagePrefix}settings#channels">
		<u>Kommunikationswegen</u></a>, wie z.B. Internetshops.
		<li><b>Zugang Kommunikationswege ändern:</b> Ändern der Zugangsmethoden zu <a
			href="${pagePrefix}settings#channels"><u>Kommunikationswegen</u></a>, wie z.B. Internet, Handy, etc. 
		<li><b>PIN entsperren:</b> Entsperren der PIN, falls das Mitglied die maximale Anzahl der Anmeldeversuche 
		überschritten hat.<br>
		<br>
	</ul>
	<br>
	<br>
	<li><b>Mitgliedszahlungen:</b> Kontrolliert, zu welchen Zahlungselementen des Mitglieds 
	der Broker Zugang hat.<br>
	Unterpunkte sind: 
	<ul>
		<li><b>Zahlung als Mitglied an Mitglied:</b> Wählen Sie hier die Zahlungstypen, die der 
		Broker ausführen kann, so als sei er ein Mitglied, das eine Zahlung an ein anderes 
		Mitglied leistet. 
		<li><b>Zahlung als Mitglied an System:</b> Der Broker kann eine Zahlung vom Mitgliedskonto
		 auf ein Systemkonto ausführen, so als sei er das Mitglied. Wählen Sie hier die Zahlungstypen, 
		 für die Sie das zulassen möchten. 
		<li><b>Autorisieren:</b> Der Broker kann Zahlungen des Mitglieds für das System 
		<a href="${pagePrefix}payments#authorized"><u>Autorisierte Zahlungen</u></a> autorisieren.
		<li><b>Autorisierte Zahlung als Mitglied annullieren</b>
		<li><b><a href="${pagePrefix}payments#scheduled"><u>Geplante Zahlung</u></a> als Mitglied annullieren</b>.
		<br>
		Damit die Einstellung funktionieren kann, muss die Konfiguration des Überweisungstyps
		auch die Annullierung von geplanten Zahlungen zulassen.
		<li><b>Geplante Zahlung als Mitglied sperren:</b> Gestattet dem Mitglied, seine geplante Zahlung 
		zeitweilig zu sperren. Der Unterschied zwischen Sperren und Annullieren ist der, dass eine Sperrung 
		wieder aufgehoben werden kann. Annullierung ist endgültig.
		<br>
		Damit die Einstellung funktionieren kann, muss die Konfiguration des Überweisungstyps
		auch die Annullierung von gesperrten Zahlungen zulassen.		
		<li><b>Mitglied Eigenzahlung:</b> Dies sind Überweisungen von einem Konto eines Mitglieds auf ein 
		anderes Konto des gleichen Mitglieds. Der Broker kann diese Zahlungen so ausführen, als sei er 
		das Mitglied. 
	</ul>
	<br>
	<br>
	<li><b><a href="${pagePrefix}member_records"><u>Mitgliedseinträge:</u></a></b>
	Ermöglicht dem Broker, die Mitgliedseinträge zu verwalten – und zu definieren, welche Information 
	über Mitglieder gesammelt wird. Unterpunkte erklären sich von selbst.<br>
	<br>
	<li><b><a href="${pagePrefix}reports#sms_log"><u>SMS-Protokolle</u></a>:</b> Ermöglicht Zugang 
	zu den Protokollen der an die gebrokerten Mitglieder gesandten SMS-Nachrichten. Das System 
	kann für das Versenden von SMS-Nachrichten zu unterschiedlichen Gelegenheiten konfiguriert 
	werden.<br>
	<br>
	<li><b>Brokering:</b>
	<ul>
		<li><b>Registrieren:</b> Ermöglicht dem Broker, ein neues Mitglied zu registrieren. 
		<li><b>Profil ändern:</b> Ermöglicht dem Broker, Veränderungen in den  <a
			href="${pagePrefix}profiles"><u>Profilen</u></a> der von ihm gebrokerten Mitglieder vorzunehmen. 
		<li><b>Standardkommission verwalten:</b> Dies ermöglicht dem Broker, die Standard-Werte der
		<a href="${pagePrefix}brokering#commission"><u>Kommissionen</u></a> selbst einzustellen.
		<li><b>Kommissionsabkommen verwalten:</b> Wird dies gewählt, kann der Broker die 
		<a href="${pagePrefix}brokering#commission_contract"><u>Kommissionsverträge</u></a> mit Mitgliedern 
		verwalten. Dies tut er über eine Schaltfläche unter dem	<a href="${pagePrefix}profiles">
		<u>Profil</u></a>.
		<li><b>Interessierte Mitglieder verwalten:</b> Der Broker kann die Interessenten zeigen, die sich 
		zwar bereits registriert haben, aber ihre Registrierung noch nicht per E-Mail bestätigt haben.<br> 
		Mehr Information dazu finden Sie <a href="${pagePrefix}user_management#search_pending_member">
		<u>hier</u></a>.
	</ul>
	<br>
	<br>
	<li><b>Persönliche Nachrichten:</b> Hier gibt es nur ein Element, das dem Broker ermöglicht, 
	persönliche <a href="${pagePrefix}messages"><u>Nachrichten</u></a> an alle von ihm gebrokerten 
	Mitglieder zu schicken.<br>
	<br>
	<li><b>Referenzen:</b> Ermöglicht dem Broker,	<a href="${pagePrefix}references">
	<u>Referenzen</u></a> für das Mitglied zu verwalten (erteilen, löschen, ändern).<br>
	<br>
	<li><b>Berichte:</b>
	<ul>
		<li><b>Ansicht:</b> Wird dies gewählt, kann der Broker   <a
			href="${pagePrefix}reports#member_reports"><u>Berichte</u></a> der von ihm gebrokerten 
			Mitglieder anzeigen. 
		<li><b>Kontoinformationen anzeigen:</b> Wenn Sie hier einen Kontotyp wählen, kann der Broker 
		ebenso Kontoinformationen (Salden) seiner Mitglieder in diesen Berichten zeigen. 
	</ul>
	<br>
	<br>
</ul>
<hr class="help">
</span>

<a name="group_filters"></a>
<p class="sub_description">
<h2>Gruppenfilter</h2>
Gruppenfilter stellen eine Art &quot;Supergruppe&quot; dar: Eine Reihe von gebündelten 
Gruppen, die mit einem Namen versehen wurden, und mit denen Sie bestimmte Aktionen 
durchführen können. Kurz gesagt ist ein Gruppenfilter eine &quot;Gruppe von Gruppen&quot;.<br> 
Gruppenfilter sind für etliche Aufgaben in Cyclos recht praktisch. Gruppenfilter dienen z.B. 
dazu, statistische Berechnungen durchzuführen, und ermöglichen Mitgliedern und Administratoren, 
verbesserte Suchen durchzuführen.  
</p>
<i>Wo ist es zu finden?</i><br>
Gruppenfilter finden Sie unter &quot;Menü: Benutzer und Gruppen > Gruppenfilter&quot;.
<hr>

<span class="admin">
<a name="group_filter"></a>
<h3>Ändern / einrichten von Gruppenfiltern</h3>
In diesem Fenster können Sie den <a href="#group_filters"><u>Gruppenfilter</u></a>
definieren oder ändern. Wenn Sie eine bestehende Gruppe ändern möchten, klicken Sie 
bitte zuerst auf die Schaltfläche &quot;Bearbeiten&quot;, damit Sie die Felder des 
Formulars ändern können.<br>
Denken Sie daran, auf &quot;Weiter&quot; zu klicken, um die Änderungen zu speichern.
<ul>
	<li><b>Name:</b> Wählen Sie den Namen, den Sie dem neuen Gruppenfilter geben möchten. 
	<li><b>Name der Anmeldeseite:</b> Diese Option erscheint nur, wenn Sie die Anmeldeseite für 
	diesen Gruppenfilter angepasst haben (im Fenster &quot;Benutzerdefinierte Dateien&quot;, unten). 
	Zugang zur benutzerdefinierten (Gruppen-)Anmeldeseite erhalten Sie, indem Sie den Namen der
	Anmeldeseite nach der &quot;globalen&quot; Anmeldeseite, gefolgt von Fragezeichen, eingeben. 
	Der Name der Anmeldeseite kann keine Leerstellen enthalten. Hier ein Beispiel:<br>
  http://www.yourdomain.org/cyclos?yourgrouploginpagename.<br>
	Bitte beachten Sie, dass es auch möglich ist, den Namen der Anmeldeseite nach 
  <a href="${pagePrefix}groups#group_details"><u>Gruppe</u></a> zu spezifizieren.
	
	<li><b>Containerseite URL:</b> Diese Einstellung wird verwendet, wenn Sie über eine Website 
	Zugriff auf Cyclos nehmen möchten. Die Einstellung funktioniert genau wie die globale 
	Containerseite (siehe  <a href="${pagePrefix}settings#local"><u>Einstellungen -	Basiseinstellungen</u></a> 
	aber nur für diesen Gruppenfilter. In dieses Feld müssen Sie die Seite setzen, die den 
	iframe oder den Frame-Set öffnet, der Cyclos enthält. Zum Beispiel: 
	http://www.yourgroupdomain.org/cycloswrapper.php.<br>
	Bitte beachten Sie, dass es auch möglich ist, eine Anmelde-Container-Seite per <a
		href="${pagePrefix}groups#group_details"><u>Gruppe</u></a> zu spezifizieren.
	
	<li><b>Beschreibung:</b> Diese Beschreibung richtet sich an Administratoren, um zu 
	dokumentieren, welche Benutzer zu diesem Gruppenfilters gehören. 
	<li><b>Im Profil anzeigen:</b> Wird dies gewählt, erscheint der Gruppenfilter anstatt
	der Gruppe im <a href="${pagePrefix}profiles"><u>Profil</u></a> des Mitglieds.
	<li><b>Gruppen:</b> Dies ist das wichtigste Element dieses Formulars. Hier wählen Sie, 
	welche Gruppen dieser Gruppenfilter beinhalten soll. 
	<li><b>Einsehbar durch:</b> Ermöglicht Ihnen zu wählen, welche Gruppen in der Lage sind, 
	Ihren Gruppenfilter zu sehen. In diesem Fall erscheint der Gruppenfilter in der Mitglieds- 
	und Inseratesuche im Mitgliedsabschnitt. Anmerkung: Haben Sie das Kontrollkästchen 
	&quot;Im Profil anzeigen &quot; gewählt, ist er im Profil stets sichtbar, unabhängig davon, 
	welche Gruppen Sie unter &quot;Einsehbar durch&quot; gewählt haben. Diese Einstellung 
	betrifft nur die Suchfunktion.
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_filters"></a>
<h3>Gruppenfilter verwalten</h3>
Dieses Fenster zeigt eine Liste mit <a href="#group_filters"><u>Gruppenfilter</u></a>, 
und ermöglicht Ihnen, diese zu verwalten. 
<ul>
	<li><b>Ändern</b> (oder anzeigen) eines bestehenden Gruppenfilters über das Bearbeiten-Symbol 
	<img border="0" src="${images}/edit.gif" width="16" height="16">.
	<li><b>Löschen</b> eines Gruppenfilters über das Löschen-Symbol <img border="0"
		src="${images}/delete.gif" width="16" height="16">.
	<li><b>Einrichten</b> eines neuen Gruppenfilters durch Anklicken der Schaltfläche 
	mit der Beschriftung &quot;Neuen Gruppenfilter einfügen&quot;. 
</ul>
<hr class="help">
</span>

<span class="admin">
<a name="manage_group_filter_customized_files"></a>
<h3>Benutzerdefinierte Dateien (für Gruppenfilter)</h3>
Mit dieser Funktion können Sie eine 
<a href="${pagePrefix}content_management"><u>benutzerdefinierte Datei</u></a>
für einen <a href="#group_filters"><u>Gruppenfilter</u></a> einstellen.
Dies bedeutet, dass jeder Gruppenfilter seine eigene Benutzerdefinition wie Layout 
(Farben, Styles), Logo, und Seiten wie z.B. Neuigkeiten (Schwarzes Brett), Kontakt, Handbuch, 
etc. haben kann. Wird keine spezifische Anpassung von Gruppen oder Gruppenfiltern vorgenommen, 
so erscheinen Hauptlayout- und Anwenderseiten. 
<br>
Gruppen-Anpassung überlagert die Gruppenfilter-Anpassung; Falls Sie also beide definiert 
haben, überprüft das System zuerst, ob eine Datei für die Gruppe angepasst wurde, und zeigt 
dann diese Anpassung. Ist dies nicht der Fall, überprüft es, ob die Datei für den Gruppenfilter 
angepasst wurde. Ist auch das nicht der Fall, zeigt es, falls verfügbar, die systemweit 
angepasste Version der Datei (Content-Management). 
<br>
Elemente des Formulars: 
<ul>
	<li><b>Anzeigen</b> wie das Ergebnis aussehen wird, über das Anzeigen-Symbol  
	  <img border="0"	src="${images}/view.gif" width="16" height="16">.
	<li><b>Ändern</b> einer bestehenden benutzerdefinierten Datei über das Bearbeiten-Symbol 
	  <img border="0"	src="${images}/edit.gif" width="16" height="16">.
	<li><b>Löschen</b> einer benutzerdefinierten Datei über das Löschen-Symbol 
	  <img border="0"	src="${images}/delete.gif" width="16" height="16">.
	<li><b>Einrichten</b> einer neuen benutzerdefinierten Datei über die Schaltfläche 
	&quot;Neue Datei anpassen&quot;.
</ul>
<hr class="help">
</span>

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

