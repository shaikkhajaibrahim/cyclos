<div style="page-break-after: always;">
<a name="top"></a>
<p class="head_description">
当指定的事件发生在联环系统时，可通过电邮或讯息让用户得到警示通知。
</p>

<span class="member">
<i>在哪里可以找到它？</i><br> 
通知可通过 &quot;菜单：偏好 > 通知&quot; 进入。
</span>

<span class="admin"> <i>在哪里可以找到它？</i><br>
通知可通过 &quot;菜单：个人 > 电邮通知&quot; 进入。
<p><i>如何使它运作？</i><br>
管理员永远可以选择配置个人的通知。<br>
管理员可以在 <a	href="${pagePrefix}groups#manage_group_permissions_member"><u>会员权限</u></a> 节内（&quot;偏好&quot; 区块中）启用会员（组别）的通知职能。<br>
附加的通知设置可在 <a href="${pagePrefix}groups#edit_member_group"><u>会员组别设置</u></a>（&quot;通知&quot; 区块）中定义。<br>
通知的内容可以通过在 &quot;菜单：翻译 > 通知&quot; 的 <a href="${pagePrefix}translation"><u>翻译</u></a> 区块中更改。<br>
</span>
<hr>

<span class="admin"><A NAME="email_notifications"></A>
<h3>电邮通知</h3>
通过下拉的复选框来选择您希望收到的 <a href="#top"><u>通知</u></a> 类型。完成后，请点击 &quot;提交&quot; 来储存更改。
<ul>
	<li><b>新增注册会员：</b> 如果会员放置在新（最初）组别，您可以得到通知。可以拣选一个或多个组别。<br>
	如果电邮确认是必须的 (在 <a href="${pagePrefix}groups#group_registration_settings"><u>会员组别设置</u></a>），当会员已确认注册时，您将收到通知。
	<li><b>付款：</b> 您可以为每个可用的 <a href="${pagePrefix}account_management#transaction_types"><u>付款方式</u></a> 设置通知。
	这就意味着当这种类型的转让发生时，您会收到通知。
	<li><b>讯息：</b> 您可以为每个 <a href="${pagePrefix}messages#categories"><u>讯息类别</u></a> 设置通知。
	<li><b>系统警示：</b> 您可以为每个 <a href="${pagePrefix}alerts_logs#system_alerts"><u>系统警示</u></a> 设置通知。
	<li><b>会员警示：</b> 您可以为每个 <a href="${pagePrefix}alerts_logs#member_alerts"><u>会员警示</u></a> 设置通知。
	<li><b>应用程序错误：</b> 拣选此复选框将会通过电邮得到 <a
		href="${pagePrefix}alerts_logs#error_log"><u>应用程序错误</u></a> 的通知。
	<li><b>系统发票：</b> 拣选此复选框将会通过电邮收到接收 <a href="${pagePrefix}invoices#top"><u>系统发票</u></a> 的通知。
</ul>
<hr class="help">
</span>

<span class="member">
<A NAME="notification_preferences"></A>
<h3>通知偏好</h3>
在此页面，您可以定义您希望收到的通知，您还可以选择通过联环系统的内部讯息、电邮或短讯（如果管理员启用了）来接收它们。但是，管理员发送的内部讯息是不能禁用的。<br>
与往常一样，您应该首先点击 &quot;更改&quot; 按钮，以进行修改；完成后，请点击 &quot;提交&quot; 来储存更改。
<p>有以下通知可用（请注意，并非所有通知会出现）：
<ul>
	<li><b>会员讯息：</b>
	这些讯息是通过联环系统发送的，无论是会员或者是管理员。此选项启用了（通过拣选电邮选项）接收电邮的方法，而不需要在联环系统中刊登您的电邮地址。
	<li><b>管理员致个人讯息</b>
	<li><b>管理员讯息：</b> 这是管理员发送的个人或大量讯息。
	<li><b>访问警示：</b> 您将收到各种使用错误密码尝试登录到您的账户的通知。
	<li><b>一般账户活动：</b> 这是有关账户的活动，如：低信贷警示。
	<li><b>经纪活动：</b> 通知有关经纪的任何活动。<span class="member">它们是：</span> 
	<ul>
		<span class="member">
		<li>新增 <a href="${pagePrefix}brokering#commission_contract"><u>佣金合同</u></a >
		<li>取消佣金合同。
		</span>
		<span class="broker">
		<li>经纪已过期 
		<li>经纪已删除/经纪已更改 
		<li>已从经纪组别删除 
		<li>等待经纪授权付款
		<li>接受 <a href="${pagePrefix}brokering#commission_contract"><u>佣金合同</u></a>  
		<li>拒绝佣金合同 
		</span>
	</ul>
	<li><b>付款活动：</b> 有关付款的活动。这将是关于收到的付款或授权和预定付款的活动。
	<li><b>通过外部渠道付款：</b> 当付款是外部进行的（如：短讯）。
	<li><b>贷款活动：</b> 这是有关 <a href="${pagePrefix}loans"><u>贷款</u></a> 的活动：有关新贷款和支付贷款到期的讯息。只有当会员有贷款，这选项才会显示。
	<li><b>广告到期警示：</b> 当广告到期。
	<li><b>广告兴趣通知：</b> 如果启用了，当新的广告符合 <a	href="${pagePrefix}ads_interest"><u>广告兴趣</u></a> 时，您将收到通知。
	<li><b>发票活动：</u></b></a> 任何有关发票的活动（如：收到、接受、取消）
	<li><b>收到评语：</b> 当收到或修改评语。
	<li><b>交易反馈意见：</b></a> 通知有关在特定交易上的质量评语。
	<li><b>担保：</b></u></a> 处理联环系统中的担保系统。
	<li><b>付款义务：</b></u></a> 处理联环系统中的付款义务系统。
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