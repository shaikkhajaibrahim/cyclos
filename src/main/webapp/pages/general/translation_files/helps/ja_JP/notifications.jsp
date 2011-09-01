<div style="page-break-after: always;"
<a name="top"></a>
<p class="head_description">
通知は、Cyclos内で指定されたイベントが生じた時に、
ユーザーがE-mailやメッセージによりアラートを受け取れるようにします。
</p>

<span class="member">
<i>どこで見つけられる？</i><br> 
通知には「メニュー: 設定 > 通知」を通じてアクセスできます。
</span>

<span class="admin"> <i>どこでそれは見つかる？</i><br>
通知には「メニュー: パーソナル > E-mail通知」を通じてアクセスできます。
<p><i>どうやって動かす？</i><br>
管理者は常にパーソナル通知を設定することができます。<br>
管理者は、<a
	href="${pagePrefix}groups#manage_group_permissions_member"><u>メンバー許可</u></a>セクション（「設定」ブロック内）でメンバー（グループ）のための通知機能を有効化できます。<br>
追加的な通知設定は、<a
	href="${pagePrefix}groups#edit_member_group"><u>メンバーグループ設定</u></a>（「通知」ブロック内）にて定められます。<br>
通知の内容は、
「メニュー: 翻訳 > 通知」を通じて、<a
	href="${pagePrefix}translation"><u>翻訳</u></a>モジュール内で変更できます。<br>
</span>
<hr>

<span class="admin"><A NAME="email_notifications"></A>
<h3>E-mail通知</h3>
ドロップダウン内のチェックボックスを選択することにより、
あなたが受け取りたい<a href="#top"><u>通知</u></a>のタイプを選択してください。
完了したら、変更を保存するために、「送信」をクリックして下さい。
<ul>
	<li><b>新しい登録済みメンバー:</b> メンバーが新しい（初期）グループに入ると、あなたに通知することができます。
	1つ以上のグループを選択することができます。<br>
	E-mail確認が（<a
		href="${pagePrefix}groups#group_registration_settings"><u>メンバーグループ
	設定</u></a>内で）必要とされている場合、あなたはそのメンバーが登録を確認した時に通知を受け取ります。
	<li><b支払い:</b> 利用可能な<a
		href="${pagePrefix}account_management#transaction_types"><u>支払いタイプ</u></a>の各々について、通知を設定できます。
	つまり、そのタイプの移転が生じた時に、あなたに通知されることを意味します。
	<li><b>メッセージ:</b> 各々の<a
		href="${pagePrefix}messages#categories"><u>メッセージカテゴリー</u></a>について、通知を設定できます。
	<li><b>システムアラート:</b> 各々の<a
		href="${pagePrefix}alerts_logs#system_alerts"><u>システムアラート</u></a>について、通知を設定できます。
	<li><b>メンバーアラート:</b> 各々の<a
		href="${pagePrefix}alerts_logs#member_alerts"><u>メンバーアラート</u></a>について、通知を設定できます。
	<li><b>アプリケーションエラー:</b> E-mailによって<a
		href="${pagePrefix}alerts_logs#error_log"><u>アプリケーションエラー</u></a>の通知を受けるには、このチェックボックスを選択してください。
	<li><b>システム請求書:</b> <a href="${pagePrefix}invoices#top"><u>
	システム請求書</u></a>の受け取りについて、E-mailによって通知を受けるには、このチェックボックスを選択してください。
</ul>
<hr class="help">
</span>

<span class="member">
<A NAME="notification_preferences"></A>
<h3>通知設定</h3>
このページでは、あなたが受け取りたい通知を定めることができ、
それらをCyclos内部メッセージで受け取るか、E-mailで受け取るか、SMS（管理者によって有効化されていれば）で受け取るかを選べます。
ただし、管理者からの内部メッセージは無効化できません。<br>
いつものように、修正をするためにまず「変更」ボタンをクリックし、完了したら、
変更を保存するために「送信」をクリックして下さい。
<p>以下の通知を利用可能です（すべてではないことに注意してください。）
<ul>
	<li><b>メンバーからのメッセージ</b>
	これらは、Cyclosを通じて、メンバーまたは管理者によって送信されたメッセージです。
	このオプションは、Cyclos内であなたにE-mailアドレスを公開することなくE-mailを受け取る方法を（E-mailオプションをチェックすることにより）有効化します。
	<li><b>管理者からのパーソナルメッセージ</b>
	<li><b>管理者からのメール:</b> これらは管理者から送信されるパーソナルメッセージまたはバルクメッセージです。
	<li><b>アクセスアラート:</b> あなたのアカウントに誤ったパスワードでログインしようとする様々な試行について通知を受け取るでしょう。
	<li><b>一般的な口座イベント:</b> これは低クレジットアラートのような、口座に関連したイベントです。
	<li><b>仲介イベント:</b> 仲介者イベントについての通知。
	<span class="member">それには以下のものがあります:</span> 
	<ul>
		<span class="member">
		<li>新しい<a href="${pagePrefix}brokering#commission_contract"><u>委託手数料契約</u></a >の挿入
		<li>委託手数料契約のキャンセル
		</span>
		<span class="broker">
		<li>仲介の期限切れ
		<li>仲介の削除／仲介者の変更
		<li>仲介者グループからの削除
		<li>仲介者の認可が必要な保留中の支払い
		<li><a href="${pagePrefix}brokering#commission_contract"><u>委託手数料契約</u></a>の承諾
		<li>委託手数料契約の拒否
		</span>
	</ul>
	<li><b>支払いイベント:</b> 支払いに関連したイベント。これは受け取った支払いまたは、認可とスケジュールされた支払いに関するイベントです。
	<li><b>外部チャネルを通じて行われた支払い:</b> 支払いが外部的に（例えば、SMSによって）なされた場合。
	<li><b>ローンイベント:</b> これは<a
		href="${pagePrefix}loans"><u>ローン</u></a>に関連したイベントです。新しいローンとローン支払いの期限切れについてのメッセージ。このオプションは、メンバーがローンを有する場合にのみ表示されます。
	<li><b>広告期限切れアラート:</b> 広告が期限切れになった場合</u></a>。
	<li><b>広告関心の通知:</b> 有効化された場合、新しい広告が<a
		href="${pagePrefix}ads_interest"><u>広告関心</u></a>に合致する場合に、あなたは通知を受け取るでしょう。
	<li><b>請求書イベント:</u></b></a> 請求書についての任意のイベント（受け取り、承諾、キャンセル）。
	<li><b>照会状の受け取り:</b> 照会状を受け取った、または、修正された場合。
	<li><b>取引フィードバック:</b></a> 1つの特定の取引に関する質の照会についての通知。
	<li><b>保証書:</b></u></a> Cyclos内の保証書システムを処理します。
	<li><b>支払債券:</b></u></a> Cyclos内の保証書システムを処理します。
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