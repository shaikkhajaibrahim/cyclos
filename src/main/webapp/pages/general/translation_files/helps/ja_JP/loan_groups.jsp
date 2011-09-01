<div style="page-break-after: always;"
<a name="top"></a>
<p class="head_description">ローングループは、複数のメンバーがローンに対して集団的に責任を持つことを可能にします。<br>
ローングループにローンを与えることは、そのローングループに責任のあるメンバーがローンを受け取ることを意味します。
すべてのメンバーは、そのローンを見ることができ、そのローンを返済することもできます。</p>
<i>どこで見つけられる？</i>
<br>
ローングループには、
<span class="admin">「メニュー: ユーザーとグループ: ローングループ」を通じてアクセスできます。<br>
この項目を見るには、管理者は正しい<a
	href="${pagePrefix}groups#manage_group_permissions_admin_member"><u>許可</u></a>（ローングループブロック）を持つ必要があります。<p>

<i>どうやって動かす？</i><br>
一度ローングループを作成すると、以下で述べられるように、
<a href="#create_loan_group"><u>ローングループを作成する</u></a><br>において、それにメンバーを追加できます。

ローングループにローンを当てるためには、ローンが存在しており、管理者がローンを与えるための<a href="${pagePrefix}groups#manage_group_permissions_admin_member"><u>
許可</u></a>（許可ブロック「ローン」内）を持っていなければなりません。<br>
メンバーグループごとに定めることができる追加的な<a href="${pagePrefix}groups#edit_member_group"><u>
ローングループ設定</u></a>があります。<br>
「メニュー: ユーザーとグループ: ローングループ」> ローングループを編集する > ローンを与える を通じてか、<a
	href="${pagePrefix}profiles#actions_for_member_by_admin"><u> </u></a>（ブロック ローン > ローンを許可する）を通じて、ローンを与えることができます。</span>


<span class="member">「メニュー: 口座 > ローングループ」</span>
<hr>

<span class="admin">
<a NAME="search_loans_group"></a>
<h3>ローングループを検索する</h3>
これは<a href="#top"><u>ローングループ</u></a>の検索ページです。<br>
あなたはローングループ名、説明、そのグループに属するメンバー（そのメンバーのログイン名または実名）によって検索できます。
<p>必要なフィールドに記入した後、「検索」をクリックすると、下の<a href="#search_loans_group_result"><u>
検索結果ウィンドウ</u></a>にそのグループが表示されます。
<p>
新しいローングループは、「<a href="#create_loan_group"><u>
ローングループを作成する</u></a>」ボタンをクリックすることにより、作成できます。<hr class="help">
</span>

<span class="admin">
<a name="create_loan_group"></a>
<h3>新しいローングループ</h3>
このウィンドウでは、新しいローングループを作成できます。
新しいグループの名称と説明を入力して、「送信」をクリックして下さい。
<p>新たに作成されたグループが、次の画面、つまり、
<a href="#search_loans_group_result"><u>ローン検索結果</u></a>ウィンドウ内に表示されます。
最初の作成時には、そのグループはまだ空です。あなたは、そのローングループを修正することにより、それにメンバーを追加できます。（ローン検索結果ウィンドウ内の<img border="0" src="${images}/edit.gif" width="16" height="16">アイコンをクリックして下さい。）
<hr class="help">
</span>

<span class="admin">
<a name="search_loans_group_result"></a>
<h3>ローングループ検索結果</h3>
ここには、<a href="#search_loans_group"><u>ローングループ検索</u></a>の結果があります。.
<a href="#top"><u>ローングループ</u></a>名と説明がひょじされます。
さらに、あなたは
<img border="0" src="${images}/edit.gif" width="16" height="16">
編集アイコンを使うことができ、それにより、そのローングループの属性の編集、
メンバーの追加、そのグループのローンの管理を行えるウィンドウへと移動できます。
<p>あなたは
<img border="0" src="${images}/delete.gif" width="16" height="16">
消去アイコンして、グループを削除することができます。
これは、そのグループに割り当てられた未決済ローンが全くない場合にのみ可能です。
<hr class="help">
</span>

<span class="admin">
<a name="loan_group_members_by_admin"></a>
<h3>このローングループ内のメンバー</h3>
このウィンドウは、ある<a href="#top"><u>ローングループ</u></a>内のメンバーを表示します。.
名前とログイン名が表示されます。<br>
グループからメンバーを削除するには、
<img border="0"
	src="${images}/delete.gif" width="16" height="16">アイコンをクリックして下さい。<br>
グループにメンバーを追加するには、編集ボックス内にログイン名または名前（名前は自動補完されます）を入力して、「追加」ボタンをクリックして下さい。
<hr class="help">
</span>

<span class="admin">
<a name="loan_group_detail_by_admin"></a>
<h3>ローングループを修正する</h3>
このウィンドウは、<a href="#top"><u>ローングループ</u></a>についての情報へのアクセスを提供します。
あなたは以下のアクションを実行できます:
<ul>
	<li><b>変更する:</b> 名称または説明を変更するには、このボタンをクリックして下さい。それらを変更した後、変更を保存するために「送信」をクリックして下さい。
	<li><b>ローンを見る:</b> このグループのための既存の
	<a href="${pagePrefix}loans"><u>
	ローン</u></a>の概観を得るには、このボタンをクリックして下さい。
	<li><b>ローンを与える:</b> そのグループにローンを与えるには、このボタンをクリックして下さい。
	そのグループに責任のあるメンバーがローンを受け取ります。
	許可が設定されている場合、そのローングループのすべてのメンバーがローンを見ることができ、それを返済することができます。
</ul>
<p>
<font color="#FF0000">注:</font> メンバー管理機能を通じてローングループにアクセスする場合、管理者は、「見る」許可のみを持っています。
ここで言及されているアクションは、直接、ローングループ管理ページ（「メニュー: ユーザーとグループ > ローングループ」）からのみ行えます。
<hr class="help">
</span>

<a name="search_loans"></a>
<h3>グループのローン...</h3>
ここでは、<a href="#top"><u>
ローングループ</u></a>のローンの概観を得ることができます。
フォームは非常に単純です:
「未決済」または「完了」のローンを見るために、
2つのラジオボタンの一方を選択するだけです。
<hr class="help">

<span class="member">
<a NAME="member_loan_groups_by_member"></a>
<h3>自分のローングループ</h3>
ここでは、あなたが属する<a href="#top"><u>ローングループ</u></a>を見ることができます。
ローングループについて、さらなる情報を見るには、
<img border="0" src="${images}/view.gif" width="16" height="16">
ビューアイコンをクリックして下さい。
<hr class="help">
</span>

<span class="admin">
<a NAME="member_loan_groups_by_admin"></a>
<h3>メンバーのローングループ</h3>
ここでは、そのメンバーが属する<a href="#top"><u>ローングループ</u></a>を見ることができます。
ローングループについて、さらなる情報を見るには、
<img border="0" src="${images}/view.gif" width="16" height="16">
ビューアイコンをクリックして下さい。<br>
ローングループを廃止するには、
<img border="0" src="${images}/delete.gif" width="16" height="16">消去アイコンをクリックして下さい。
これはそのローングループに未決済のローンがない場合にのみ可能です。<p>
ここではローンを見られないことに注意して下さい。
ローンを見るには、メンバープロフィール > ローンを見る か、「メニュー: 口座 > ローンの管理」に行って下さい。
<hr class="help">
</span>

<span class="member">
<a name="loan_group_detail_by_member"></a>
<h3>ローングループの詳細</h3>
ここでは、ローングループ名と説明が表示されます。<br>
このグループ内のメンバーは、<a href="#loan_group_members_by_member"><u>
下のウィンドウ</u></a>内に表示されます。
<hr class="help">
</span>

<span class="admin">
<a name="add_member_loan_groups"></a>
<h3>ローングループにメンバーを追加する</h3>
このウィンドウを使って、<a href="#top"><u>ローングループ</u></a>にメンバーを追加できます。
メンバーは複数のローングループに属することができます。
フォームは非常に単純です:
ただローングループを選択して、「送信」をクリックして下さい。
<hr class="help">
</span>

<span class="member">
<a NAME="loan_group_members_by_member"></a>
<h3>このローングループ内のメンバー</h3>
ここには、このローングループのメンバーの実名とログイン名が表示されます。
それらをクリックすると、そのメンバーの<a href="${pagePrefix}profiles"><u>
プロフィール</u></a>に行くことができます。
ここではあなたのローン（またはそのローングループに対するローン）を見ることはできません。
したがって、「メニュー: 口座 > ローン」に行って下さい。これにより、あなたがメンバーであるローングループに対するローンも表示されます。
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