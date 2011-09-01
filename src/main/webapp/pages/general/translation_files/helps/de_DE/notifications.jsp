<div style="page-break-after: always;">
<a name="top"></a>
<p class="head_description">
Benachrichtigungen ermöglichen es, Benutzer per E-Mail oder interner Nachricht über 
bestimmte Ereignisse in Cyclos zu informieren. 
</p>

<span class="member">
<i>Wo ist es zu finden?</i><br>
Benachrichtigungen sind zugänglich über das &quot;Menü: Benachrichtigungen > Benachrichtigungsoptionen&quot;.  
</span><br>
<span class="admin"> <i>Wo ist es zu finden?</i><br>
Benachrichtigungen sind zugänglich über das &quot;Menü: Persönlich > E-Mail-Benachrichtigungen&quot;. 

<p><i>Wie werden Benachrichtigungen aktiviert?</i><br>
Administratoren haben jederzeit die Option, persönliche Benachrichtigungen zu konfigurieren.<br>
Ein Administrator kann die Benachrichtigungsfunktion für Mitglieder (Gruppen) in der Sektion  <a
	href="${pagePrefix}groups#manage_group_permissions_member"><u>Gruppe Berechtigungen</u></a> unter &quot;Benachrichtigungen&quot; aktivieren.<br>
Zusätzliche Benachrichtigungseinstellungen können in  <a
	href="${pagePrefix}groups#edit_member_group"><u>Gruppe Einstellungen</u></a> (unter &quot;Einstellungen Benachrichtigungen&quot;) definiert werden.<br>
Der Inhalt der Benachrichtigungen kann im Modul <a	href="${pagePrefix}translation"><u>Übersetzung</u></a> 
geändert werden, über &quot;Menü: Übersetzung > Benachrichtigungen&quot;.<br>
</span>
<hr>

<span class="admin"><A NAME="email_notifications"></A>
<h3>E-Mail-Benachrichtigungen</h3>
Wählen Sie die <a href="#top"><u>Benachrichtigungen</u></a>, die Sie erhalten möchten, 
indem Sie die Kontrollkästchen im Listenfeld wählen. Nachdem Sie dies getan haben, klicken 
Sie bitte auf &quot;Weiter&quot;, um die Änderungen zu speichern.
<ul>
	<li><b>Neu registrierte Mitglieder:</b> Falls ein neues Mitglied einer Startgruppe hinzu gefügt 
	wird, können Sie benachrichtigt werden. Eine oder mehrere Gruppen können ausgewählt werden.<br>
	Falls in <a	href="${pagePrefix}groups#group_registration_settings"><u>Gruppen Einstellungen</u></a>
	(Mitgliedergruppe) eine E-Mail-Bestätigung erforderlich ist, erhalten Sie die Benachrichtigung, 
	sobald das Mitglied die Registrierung bestätigt hat. 
	<li><b>Zahlungen:</b> Für jede der verfügbaren  <a
	href="${pagePrefix}account_management#transaction_types"> <u>Überweisungstypen</u></a> 
  können Sie eine Benachrichtigung einrichten: das bedeutet, dass Sie immer dann benachrichtigt werden, 
  wenn eine Überweisung dieses Typs getätigt wird. 
	<li><b>Neue Zahlung die Autorisierung erwartet:</b>	Für jeden der verfügbaren <a
        href="${pagePrefix}account_management#transaction_types"><u> autorisierbaren Überweisungstypen</u></a> 
        können Sie eine Benachrichtigung einstellen, was bedeutet, dass Sie benachrichtigt werden, wenn eine
        Überweisung dieses Typs autorisiert wird und stattfindet.	
	<li><b>Sicherheiten:</b> Wählen Sie hier, über welche Sicherheitstypen Sie benachrichtigt werden möchten, 
	wenn eine neue <a href="#guarantees"><u>Sicherheit</u></a> dieses Typs eingerichtet wird.
	<li><b>Nachrichten:</b> für jede <a href="${pagePrefix}messages#categories"><u>
	Nachrichten-Kategorie</u></a> können Sie die Benachrichtigung einstellen.
	<li><b>Systemmeldungen:</b> für jede der <a	href="${pagePrefix}alerts_logs#system_alerts"><u> Systemmeldungen</u></a> 
	können Sie die Benachrichtigung einstellen. 
	<li><b>Mitgliedsmeldungen:</b> Für jede der <a
	href="${pagePrefix}alerts_logs#member_alerts"><u>Mitgliedsmeldungen</u></a> 
	können Sie die Benachrichtigung einstellen. 
	<li><b>Anwendungsfehler:</b> Wählen Sie dieses Kontrollkästchen, um per E-Mail über  <a
	href="${pagePrefix}alerts_logs#error_log"><u>Anwendungsfehler</u></a> informiert zu werden. 
	<li><b>Systemrechnungen:</b> Wählen Sie dieses Kontrollkästchen, um per E-Mail über den Erhalt von
	<a href="${pagePrefix}invoices#top"><u>Systemrechnungen</u></a> informiert zu werden.
</ul>
<hr class="help">
</span>

<span class="member">
<A NAME="notification_preferences"></A>
<h3>Benachrichtigungsoptionen</h3>
Auf dieser Seite können Sie definieren, welche Benachrichtigungen Sie erhalten möchten und auf 
welche Art: interne Cyclos-Nachricht, per E-Mail oder SMS (falls von der Administration freigegeben). 
Interne Nachrichten von der Administration können allerdings nicht gesperrt werden.<br>
Wie sonst auch, klicken Sie bitte zuerst auf die Schaltfläche &quot;Bearbeiten&quot;, um Änderungen 
vorzunehmen. Wenn Sie fertig sind, klicken Sie bitte auf &quot;Weiter&quot;, um die Änderungen zu speichern.
<p>Die folgenden Benachrichtigungen sind verfügbar: 
<ul>
	<li><b>Nachrichten von Mitgliedern</b>
  Diese Nachrichten werden über Cyclos versandt, entweder von Mitgliedern oder von Administratoren. 
  Diese Option ermöglicht Ihnen, E-Mails zu erhalten (indem Sie die E-Mail-Option wählen), 
  ohne dass Ihre E-Mail-Adresse in Cyclos veröffentlicht wird. 
	<li><b>Persönliche Nachrichten von der Administration </b>
	<li><b>E-Mails von der Administration:</b> Dies sind von der Administration 
	versandte persönliche Nachrichten oder Massenmitteilungen.  
	<li><b>Zugangs-Meldungen:</b> Sie erhalten eine Benachrichtigung über mehrmalige 
	Anmeldungsversuche mit ungültigem Kennwort zu Ihrem Konto. 
	<li><b>Allgemeine Kontovorgänge:</b> Diese Benachrichtigungen stehen in Verbindung mit 
	Konto-Vorgängen, wie z.B. niedriges Guthaben.  
	<li><b>Brokering-Vorgänge:</b> Benachrichtigungen zu Brokering-Vorgängen.
	<span class="member">Diese sind:</span> 
	<ul>
		<span class="member">
		<li>Neues <a href="${pagePrefix}brokering#commission_contract"><u>Kommissionsabkommen</u></a > erstellt
		<li>Kommissionsabkommen gekündigt
		</span>
		<span class="broker">
		<li>Brokering abgelaufen 
		<li>Brokering entfernt / Broker wurde geändert 
		<li>Aus Brokergruppe entfernt 
		<li>Anstehende Zahlung bedarf Autorisierung durch Broker 
		<li><a href="${pagePrefix}brokering#commission_contract"><u>Kommissionsabkommen</u></a> akzeptiert 
		<li>Kommissionsabkommen abgelehnt 
		</span>
	</ul>
	<li><b>Zahlungsvorgänge:</b> Vorgänge in Zusammenhang mit Zahlungen. Hierbei handelt 
	es sich um eingegangene Zahlungen, oder aber um Autorisierung und geplante Zahlungen.
	<li><b>Zahlung über externe Kommunikationswege:</b> Für extern getätigte Zahlungen (z.B. per SMS)
	<li><b>Darlehensvorgänge:</b> Dies sind Vorgänge in Zusammenhang mit 
	<a href="${pagePrefix}loans"><u>Darlehen</u></a>. Nachrichten zu neuen Darlehen oder der Ablauf 
	von Darlehenszahlungen. Diese Option wird nur dann gezeigt, wenn das Mitglied Darlehen erhalten hat. 
	<li><b>Inserat-Ablauf:</b> Wenn ein Inserat abläuft</u></a>
	<li><b>Benachrichtigungen Inserat-Beobachter:</b> Wird dies aktiviert, so erhalten Sie 
	Benachrichtigungen, wann immer ein Inserat mit den Kriterien Ihres  
	<a href="${pagePrefix}ads_interest"><u>Inserat-Beobachters</u></a> übereinstimmt.
	<li><b>Rechnungsvorgänge</u></b></a> Alle Vorgänge in Zusammenhang mit Rechnungen (erhalten, akzeptiert, widerrufen).
	<li><b>Erhaltene Referenzen</b> Referenz wurde erhalten oder geändert
	<li><b>Bewertung von Geschäftsvorgängen</b></a> Benachrichtigungen zum Feedback 
	eines bestimmten Geschäftsvorgang betreffend
	<li><b>Sicherheiten</b></u></a> beziehen sich auf das Cyclos-Sicherheits-System.
	<li><b>Zahlungsverpflichtungen</b></u></a> beziehen sich auf das Cyclos-Zahlungsverpflichtungs-System.
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