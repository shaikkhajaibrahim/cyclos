<div style="page-break-after: always;">
<a name="notifications_top"></a>
<p class="head_description">

Las Notificaciones permiten a los usuarios estar informados vía correo electrónico, mensajería interna o SMS, 
cuando se producen eventos específicos en el sistema.
</p>

<span class="member">
<i>¿Dónde las encuentro?</i><br> 
A las Notificaciones se puede acceder a través del "Menú: Preferencias> Notificaciones".
</span>

<span class="admin"> <i>¿Dónde las encuentro?</i><br>
A las Notificaciones se puede acceder a través del "Menú: Personal> Notificaciones por correo electrónico".

<p><i> ¿Cómo hacerlas funcionar? </i><br>
Los administradores tendrán siempre la opción de configurar las notificaciones personales. <br>
Un administrador puede habilitar la función de notificación para los miembros (grupos) en los<a
href = "${pagePrefix}groups#manage_group_permissions_member"><u> permisos de miembros
</u></a> (en el bloque "Preferencias").<br>
Ajustes adicionales de notificación pueden ser definidos en los <a
href = "${pagePrefix}groups#edit_member_group"><u> ajustes de grupo </u></a> (en el bloque "Notificaciones").<br>
El texto de las notificaciones se puede cambiar en la <a
href = "${pagePrefix}translation"><u> traducción </u></a> a través del
"Menú: Traducción> Notificaciones".<br>
</span>
<hr>

<span class="admin"><A NAME="email_notifications"></A>
<h3>Notificaciones por correo electrónico</h3>
Seleccione los tipos de <a href="#notifications_top"><u>notificaciones</u></a> que usted desea recibir, 
seleccionando las opciones deseadas (en cada menú desplegable), 
o marcando las casillas de verificación correspondientes.<br>
<br>
Cuando finalice, haga click en el botón "Aceptar" para confirmar los cambios.<br>
<ul>
	<li><b> Nuevos miembros registrados: </b> Si un nuevo miembro ingresa a un "grupo inicial" (usuarios pendientes), 
	usted puede ser notificado.<br>
	Pueden ser seleccionados uno o más grupos (iniciales).<br>
	Si el correo electrónico de validación es obligatorio (configuración del grupo),
	usted recibirá la notificación cuando el miembro haya confirmado su registro.

	<li><b> Pagos: </b> Para cada uno de los 
	<a	href="${pagePrefix}account_management#transaction_types"><u>tipos de transacciones</u></a> disponibles,
	se puede establecer un proceso de notificación. Esto significa que se notifique cuando se lleve a cabo 
	una transferencia (pago) de este tipo.
	
	<li><b> Nuevo pago esperando por autorización: </b> Para cada uno de los tipos de transacciones que 
	necesitan ser autorizadas por la Administración, usted puede establecer un proceso de notificación.<br> 
	Esto significa que sea notificado cuando exista una nueva transferencia (pago) de este tipo pendiente de autorización.
	
	<li><b> Garantías: </b> Seleccione los tipos de garantías por los cuales quiere ser notificado 
	al momento de crear una nueva <a href="#guarantees"><u>garantía</u></a>.
	
	<li><b> Mensajes: </b> Para cada una de las 
	<a href="${pagePrefix}messages#categories"><u>categorías de mensajes</u></a>
	puede configurarse su correspondiente notificación.
	
	<li><b> Alertas del sistema: </b> Para cada una de las 
	<a href="${pagePrefix}alerts_logs#system_alerts"><u>alertas del sistema</u></a> 
	se puede establecer su correspondiente notificación.
	
	<li><b> Alertas de miembro: </b> Para cada una de las 
	<a href="${pagePrefix}alerts_logs#member_alerts"><u>alertas de miembro</u></a> 
	se puede establecer su correspondiente notificación.
	
	<li><b> Errores de aplicación: </b> Seleccione esta casilla de verificación para que se le informe acerca de los
	<a href="${pagePrefix}alerts_logs#error_log"><u>errores de aplicación</u></a> 
	ocurridos por correo electrónico.
	
	<li><b> Facturas del sistema: </b> Seleccione esta casilla para ser notificado vía correo electrónico
	de la recepción de <a href="${pagePrefix}invoices#notifications_top"><u>Facturas del sistema</u></a>.
</ul>
<hr class="help">
</span>

<span class="member">
<A NAME="notification_preferences"></A>
<h3> Preferencias de notificación </h3>
En esta página usted puede definir las Notificaciones que desea recibir en el sistema. <br>
Se puede optar por recibirlas vía: 
<ul>
	<li><b>Correo electrónico</b>: A través del e-mail registrado en su perfil.</li>
	<li><b>Mensaje Cyclos</b>: A través del sistema de mensajería interna de Cyclos.</li> 
	<li><b>Mensaje SMS</b>: A través de mensajes SMS (si está habilitado por la administración).</li>
</ul>
Los mensajes internos de la administración NO pueden ser desactivados. <br>
<br>
Como siempre, primero debe hacer click en el botón "Modificar", con el fin de realizar las modificaciones deseadas. <br>
Cuando finalice, haga click en "Aceptar" para confirmar los cambios efectuados.
<p> 
Las siguientes notificaciones están disponibles:
<ul>
	<li> <b> Mensajes de miembros:</b> Se trata de mensajes enviados a través de Cyclos, ya sea de miembros o de administradores. 
	Esta opción posibilita una forma de recibir mensajes de correo electrónico
	(con la opción de correo electrónico), sin la necesidad de publicar su dirección de correo electrónico en Cyclos.
	<li> <b> Mensajes personales de administración</b>
	<li> <b> Mensajes de administración:</b> Estos son mensajes personales o de distribución masiva, enviados por
	la administración.
	<li> <b> Alertas de acceso:</b> Usted recibirá la notificación de que realizó varios intentos para acceder a su cuenta
	con una contraseña equivocada.
	<li> <b> Eventos generales de cuenta: </b> Estos son los eventos generales relacionados con una cuenta.
	<!-- <li> <b> Intermediación eventos: </b> Notificaciones sobre cualquier evento de intermediación. -->
	<span Class="member"> Estos son: <br> </span>
	<ul>
	<span Class="member">
	<li> Nuevo <a href="${pagePrefix}brokering#commission_contract"><u>contrato de comisión</u></a>.
	<li> Contrato cancelado.<br>
	<br>
	</span>
	<span Class="broker">
	<li> Intermediación de vencimiento.
	<li> Intermediación eliminada / broker cambiado.
	<li> Broker (corredor) de grupo.
	<li> Pago broker en espera requiere autorización.
	<li> <a href="${pagePrefix}brokering#commission_contract"><u>Contrato de comisión</u></a> aceptado
	<li> Contrato de comisión negado.
	</span>
	</ul>
	<li> <b> Pagos recibidos: </b> Eventos relacionados con los pagos en el sistema. Pagos recibidos o
		eventos relacionados con la autorización y pagos previstos.
	<li> <b> Pagos realizados por canales externos: </b> Cuando se efectúa un pago
		a través de un canal externo (por ejemplo, por SMS).
	<li> <b> Eventos de préstamo: </b> Estos son los eventos relacionados a los 
	<a href="${pagePrefix}loans"><u>préstamos</u></a>. Son mensajes acerca de los nuevos préstamos y
		los respectivos vencimientos de sus pagos. 
	<li> <b> Alerta de vencimiento de anuncio: </b> Notifica la expiración de un anuncio </u></a>.
	<li> <b> Notificaciones de intereses de anuncio: </b> Si está activado, recibirá
		la notificación de que un nuevo anuncio coincide con sus 
		<a href="${pagePrefix}ads_interest"><u>intereses de anuncios</u></a>.
	<li> <b> Eventos de facturas: </u> </b> </a> Todo sobre las facturas (recibidas, aceptadas, canceladas).
	<li> <b> Referencia recibida:</b> Cuando una referencia es recibida o modificada.
	<li> <b> Certificaciones:</b> Cuando una certificación es otorgada por un emisor de garantías.
	<li> <b> Garantías: </b> </u></a> Notifica sobre el sistema de garantías en Cyclos.
	<li> <b> Obligaciones de pago: </b> </u></a> Notifica sobre el sistema de obligaciones de pago en Cyclos.
	<li> <b> Calificación de transacciones: </b> </a> Notificaciones sobre la calidad de las calificaciones recibidas en una
		transacción específica.
</ul>

<p>
<h3> Mensajes SMS </h3>
En este sector usted puede habilitar (o deshabilitar) la operación en el sistema por el canal SMS y 
seleccionar sus preferencias.<br>
<br>
Opciones disponibles:
<ul>
	<li>Habilitar operaciones por canal SMS.</li>
	<li>Autorizar cobro de paquete de mensajes adicionales cuando no se disponga de saldo.</li>
	<li>Habilitar envío de mensajes de difusión sin cargo desde la administración.</li>
	<li>Habilitar envío de mensajes de difusión con cargo desde la administración.</li>
</ul>

Adicionalmente, se despliega información correspondiente a su "estado" para el canal SMS en el sistema, dónde se indica:
<ul type="circle">
	<li>Cantidad de mensajes SMS sin cargo utilizados y disponibles.</li>
	<li>Cantidad de mensajes SMS adicionales utilizados y disponibles.</li>
	<li>Costo, tamaño y período de validez del paquete de mensajes SMS adicionales.</li>	
</ul>
<br>
El botón "Desactivar SMS", ubicado en la parte inferior izquierda de la ventana, le permite de forma rápida y sencilla, 
desactivar en su totalidad la operación SMS (y sus notificaciones) en el sistema, con un solo click.
</p>

<p></p>
<hr class="help">
</span>

</div>

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