<div style="page-break-after: always;">
<span class="admin">
<p class="head_description">
联环系统支持不同的语言，并允许您管理翻译。所有显示给最终用户的文字，是载在具体语言的文件中（每个语言一份），再加上数量有限的静态文本文件，装载较大的文本块。<br>
联环系统附有一些语言可供选择；通过设置菜单，您可以更改安装语言，只需点击几下鼠标。<br>
如果您不满意联环系统附有的翻译，您也可以修改联环系统的个别文字或词语。这个模块还包含一节，通过联环系统，维护所有发出电邮用的讯息和所有发送的电邮通知。</p>
<i>在哪里可以找到它？</i><br>
翻译模块可通过 &quot;菜单：翻译&quot;。有以下子项：
<ul>
	<li><b><a href="#translation_keys"><u>应用程序</u></a>：</b>
	可让您管理所有短小文本词语的翻译，正如它们出现在应用程序的视窗。
	<li><b><a href="#notifications"><u>通知</u></a>：</b>
	让您管理系统发送的通知。
	<li><b>电邮：</b> 允许您管理系统发送的电邮文本。
	<li><b>导入/导出：</b> 可以让您导入或导出翻译文件。这使得联环系统社区与其他联环系统的用户分享他们的翻译。
</ul>
注 1：静态文本文件，如联系和新的网页，不通过翻译模块管理；管理它们，可通过 <a
	href="${pagePrefix}content_management"><u>内容管理</u></a>。
<p>
注 2：这里不可以更改联环系统的语言。语言可以改变自 <a
	href="${pagePrefix}settings#local"><u>菜单：设置 > 本地设置</u></a> 的 &quot;国际化&quot; 区块。<br> 
<hr>

<a name="translation_keys"></a>
<p class="sub_description">
<h2>翻译关键词</h2>
任何出现在联环系统接口的短小书面文字是储存在一份语言文件内；每个语言一份。联环系统接口允许您取代整份语言文件，或者修改文件中的个别关键词。
</p>
如果一个应用程序网页内的 <a href="#key"><u>关键词</u></a> 在 <a href="#language_file"><u>语言文件</u></a> 找不到，在页面显示的关键词会出现在问号之间。
这通常看起来就像这样：&quot;???translationMessage.search.showOnlyEmpty???&quot;。在这种情况下，您可以增加关键词（和它的数值）在语言文件中，通过
&quot;菜单：翻译 > 应用程序&quot;。
<p>如果您不满意在浏览器视窗中出现的翻译，您可以这样修改它：
<ol>
	<li>去翻译关键词的搜索网页（通过 &quot;菜单：翻译 > 应用程序&quot;)，而且您可以在 &quot;数值&quot; 编辑框输入要修改的文字。
	这个关键词是不区分大小写，并且如果您只输入部分的关键词，应用程序还将寻找匹配的词语。点击
	&quot;搜索&quot;，以显示结果。
	<li>在结果页面上，使用修改图标（<img border="0" src="${images}/edit.gif" width="16" height="16">）更改该数值。
</ol>
<p>您也可以从文件增加或删除整对关键词/数值。这是有点棘手，您可能需要阅读 <a href="#caution"><u>注意事项</u></a>。
<hr>

<a name="caution"></a>
<h3>增加/删除语言关键词注意事项</h3>
联环系统的接口让您可以从 <a href="#language_file"><u>语言文件</u></a> 修改、增加或删除语言 <a href="#key"><u>关键词</u></a>。
修改这些关键词是一个安全的行动，但增加或删除关键词可有点棘手。如果您完全了解语言关键词在系统如何运作，才应该这样做。<br>
删除一个关键词，仅仅是从语言文件删除，不是从应用程序的网页删除。
当您删除关键词，如果它仍然在应用程序网页使用，下一次该网页将只显示相关的关键词名称标志，通常是不很漂亮的（例如：&quot;???about.bla.something.title???&quot;）。<br>
反之亦然：在语言文件增加了关键词并不帮您什么，如果您不开始使用它。为此，您可以 <a href="${pagePrefix}content_management"><u>自定应用程序网页</u></a>。<br>
也可能发生关键词是在更新后失去了—虽然这是非常罕见的。正常联环系统更新将增加新的翻译关键词。在这种情况下，您可以放心地增加关键词。
<hr class="help">


<A NAME="application"></A>
<h3>搜索翻译关键词</h3>
在此视窗中您可以搜索 <a href="#key"><u>翻译关键词</u></a>。<br>
在适当的修改框输入关键词或 <a href="#value"><u>数值</u></a>。请注意，搜索不区分大小写，而且您不必输入完整的词；搜索将包括部分匹配。
一如往常，您可以不输入任何东西，只需点击 &quot;搜索&quot; —这将返回所有可用的关键词/数值的结果。<br>
选择 &quot;只空值&quot; 复选框，将仅显示没有翻译的关键词（可能在更新后发生）。<br>
搜索结果将出现在下方的 <a href="#application_results"><u>搜索结果列表视窗</u></a>。在此视窗中，您有可能改变每个关键词的翻译。
<p>还可以增加新的翻译关键词；如果您想这样做，单击提交标记为 &quot;新增关键词&quot; 的按钮。您可能需要首先阅读关于这个的 <a href="#caution"><u>注意事项</u></a>。
<p>
注：这不是您想更改联环系统的语言的地方。语言可以在 <a href="${pagePrefix}settings#local"><u>菜单：设置 > 本地设置</u></a> 的 &quot;国际化&quot; 区块改变。<br> 
<hr class="help">

<a name="application_results"></a>
<h3>S搜索结果（翻译关键词/数值）</h3>
此视窗显示您在 <a href="#application"><u>上面视窗</u></a> 定义搜索的结果。<br>
在此视窗中您可以选择一个关键词，并删除它（通过 <img border="0"
	src="${images}/delete.gif" width="16" height="16">&nbsp;删除图标），或修改它（通过 <img border="0"
	src="${images}/edit.gif" width="16" height="16">&nbsp;编辑图标）。
	如果您想删除多个关键词，您可以选择一个或一个以上的复选框，然后使用 &quot;删除选定&quot; 按钮。<p>
请注意，删除关键词可能会非常棘手，您可能需要首先阅读 <a href="#caution"><u>注意事项</u></a>。
<hr class="help">

<A NAME="edit_key"></A>
<h3>修改翻译关键词</h3>
此视窗中您可以修改翻译 <a href="#key"><u>关键词</u></a> 的 <a href="#value"><u>数值</u></a>。首先点击 &quot;更改&quot;，然后进行更改，然后单击 &quot;提交&quot; 储存更改。<br>
您可以使用多行，但通常这是被忽视，其结果将显示在一个单一行内。
<hr class="help">

<A NAME="insert_key"></A>
<h3>新增翻译关键词</h3>
在这里您可以输入一个新的翻译 <a href="#key"><u>关键词</u></a> 和 <a href="#value"><u>数值</u></a>。
只要输入他们，并点击 &quot;提交&quot;。在增加新的翻译关键词之前，我们建议您阅读
<a href="#caution"><u>注意事项</u></a>  这个问题。
<hr class="help">

<A NAME="import_file"></A>
<h3>导入/导出翻译文件</h3>
在此视窗中您可以 <a href="#import"><u>导入</u></a> 或 <a href="#export"><u>导出</u></a> <a href="#language_file"><u>语言文件</u></a>。请跟除联系以获取更多信息。
<hr class="help">

<a name="import"></a>
<h3>导入语言文件</h3>
在此视窗中的上述矩形是导入一个新的 <a
	href="#language_file"><u>语言文件</u></a>。这将是一个罕见的例子，例如在联环系统增加一个新的语言。正常联环系统的自动更新会增加新的翻译 <a href="#key"><u>关键词</u></a>（如果有）。
<p>首先，您将需要决定 &quot;做什么&quot;。有四个选项：
<ul>
	<li><b>只导入新的关键词：</b> 只导入新关键词，不会影响现有的关键词。
	<li><b>只导入新的关键词和空键：</b> 和上述一样，但现在它也将导入空键（即：关键词的数值是空的，可能是因为翻译是尚未完全完成）。
	<li><b>导入新的和修改了的关键词：</b> 导入新的和修改了的关键词。这意味着，如果您之前修改了一些关键词的数值，他们将被“默认”值覆盖。不再使用的关键词将被删除。
	<li><b>取代整个文件：</b> 它会简单地覆盖整个文件。所有您之前的修改当然会丢失。
</ul>
之后，您将要 &quot;浏览&quot; 在您的计算机存储的翻译文件，并点击 &quot;提交&quot;。
<p>注：您要导入的文件没有必要包含所有关键词—除非您选择 &quot;取代整个文件&quot;。在所有其他情况下，它可以是任何数量的关键词（但必须是正确的格式）。<br>
当您要取代整个文件，确保您导入整个文件。否则，您可能失去现有的关键词。

<h3>导出语言文件</h3>
导出目前的 <a href="#language_file"><u>语言定义</u></a> 非常简单：只需使用标记为 &quot;导出属性文件&quot; 的 &quot;提交&quot; 按钮。如果您按一下这个按钮，浏览器将接管，并通常询问您是否要储存文件。<br>
导出语言文件将是罕见的情况；您想这样做，通常是您想把自己做的联环系统翻译提供给其他联环系统社区用户。
如果您做了自己的翻译，我们鼓励您与我们联系，因此我们可以在正式的联环系统增加发布您的翻译。见项目的联系地址（<a href="http://project.cyclos.org"><u>http://project.cyclos.org</u></a>）。
<hr class="help">

<a name="notifications"></a>
<h2>通知</h2>
联环系统可以让您管理各种通知的内容，如下视窗。
<hr>

<A NAME="general_notifications"></A>
<h3>一般通知</h3>
此视窗可以显示一般的 <a href="${pagePrefix}notifications"><u>通知</u></a>。通常这些是加入到发出的电邮的前缀和后缀。
您可以点击编辑图标（<img border="0" src="${images}/edit.gif" width="16" height="16">）以更改内容。
<hr class="help">

<A NAME="member_notifications"></A>
<h3>会员通知</h3>
此视窗会显示在各种场合发送给会员的 <a href="${pagePrefix}notifications"><u>通知</u></a>。
您可以点击编辑图标（<img border="0" src="${images}/edit.gif" width="16" height="16">）以更改内容。
<hr class="help">

<A NAME="admin_notifications"></A>
<h3>管理员通知</h3>
此视窗会显示在各种场合发送给管理员的 <a href="${pagePrefix}notifications"><u>通知</u></a>。
您可以点击编辑图标（<img border="0" src="${images}/edit.gif" width="16" height="16">）以更改内容。
<hr class="help">

<A NAME="edit_notifications"></A>
<h3>编辑通知</h3>
该视窗可让您变更通知的内容。要做到这一点，您（一如既往）首先应该点击 &quot;更改&quot;；完成后，您可以点击 &quot;提交&quot; 储存您的更改。<br>
丰富文本编辑器会出现，让您能够使用一些布局功能。您也可以使用某些字段，这取决于您要修改的通知。
<%-- Hoe kan ik weten welke?? --%>
<hr class="help">

<A NAME="mail_translation"></A>
<h3>电邮翻译</h3>
该视窗可让您变更在某些场合发送的电邮内容。要做到这一点，您（一如既往）首先应该点击 &quot;更改&quot;；完成后，您可以点击 &quot;提交&quot; 储存您的更改。<br>
目前，下面的电邮可以改变：
<ul>
	<li><b>邀请电邮：</b> 通过 &quot;菜单：主页 > 邀请&quot; 使用选项 <a href="${pagePrefix}home#home_invite"><u>邀请朋友</u></a> 时发送的电邮。
	<li><b>激活电邮：</b> 当会员被激活时发送给他的电邮。
	这种情况在注册后，当管理员激活账户，将会员从 &quot;<a href="${pagePrefix}groups#pending_members"><u>待审批会员</u></a>&quot; 组别转移到另一组别（通常为正式会员）时发送。
	<li><b>公众注册电邮验证：</b> 当可能的会员尝试首次注册时发送给他的电邮。要发送此电邮，联环系统必须为这个 <a href="${pagePrefix}notifications"><u>配置</u></a>。
	<li><b>重设密码电邮：</b> 密码被重置时发送的电邮。
</ul>
在所有这些定义，您可以使用字段显示文本的数据。
<%-- hoe?? welke?? --%>
<hr class="help">

<a name="imexport_notifications_mails"></a>
<h3>导入/导出通知和电邮翻译</h3>
有了这个视窗，您可以导入或导出电邮和通知的翻译文本文件。该文件是XML格式的，并且可以由联环系统任何目前（或未来）的版本读取。<br>
您这样做，可能是希望与其他联环系统之间共享翻译文本，或者出于安全原因。<br>
方法相当简单。当您选择 &quot;导入&quot;，您应该通过 &quot;浏览&quot; 按钮指定文件。如果导出文件，浏览器将接管，并要求您在哪里储存下载。
<hr class="help">

<p><a name="glossary"></a>
<h2>术语表</h2>
<p></p>

<a name="language_file"></a> <b>语言文件</b>
<p>在联环系统中，每种语言有语言文件。该文件包含所有在联环系统接口出现的书面文本 &quot;字符串&quot;（除非是大块的文本，会放在完整的文件内）。<br>
语言文件是根据特定的模式命名：
&quot;ApplicationResources_xx_XX.properties&quot;，其中 xx 是语言代码，XX 是国家代码。例如：
&quot;ApplicationResources_en_US.properties&quot; 的文件是美国英语。<br>
语言文件包含 <a href="#key"><u>关键词</u></a> 和 <a href="#value"><u>数值</u></a>，由 &quot;=&quot; 分开，中间没有任何空间。
<hr class='help'>

<a name="key"></a> <b>翻译关键词</b>
<p>翻译关键词被放置在应用网页中；当这些网页显示在您的浏览器时，关键词是从 <a href="#language_file"><u>语言文件</u></a> 中找出，并代之以相应的 <a href="#value"><u>数值</u></a>。
<hr class='help'>

<a name="value"></a> <b>翻译数值</b>
<p>翻译数值和词语是在您的浏览器的联环系统网页内看到您自己的语言翻译。原来的网页不包含这些数值。代替的是被放置在应用网页中的翻译 <a href="#key"><u>关键词</u></a>；
这些网页显示在您的浏览器时，关键词是从 <a href="#language_file"><u>语言文件</u></a> 中找出，并代之以相应的数值。<br>
所有翻译数值（应用翻译、通知和电邮）可以包含 &quot;变量&quot;。变量总是由两个 # 包围，例如 #member#、#title# 和 #amount#。变量将在联环系统改为正确的数值来显示。
变量名称是不解自明的，所有可能的变量都会使用默认的翻译数值。
<hr class='help'>

</span>

</div> <%--  page-break end --%>