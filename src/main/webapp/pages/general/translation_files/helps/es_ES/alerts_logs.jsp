<div style="page-break-after: always;">
<a name="alerts_logs_top"></a>
<span class="admin"> 
<p class="head_description">

Las Alertas son utilizadas con el fin de notificar a los administradores sobre diversos eventos ocurridos en Cyclos, 
como errores de sistema o acontecimientos en relación a los miembros 
(cantidad máxima de errores permitidos en el inicio de sesión, préstamo vencido, código de seguridad bloqueado, etc.) 
</p>

<i>¿Dónde las encuentro?</i><br>
Las alertas y registros pueden ser accededidos a través del "Menú: Alertas". <br>
<p> 
<i> ¿Cómo hacerlas funcionar?</i><br>
Las <a href="${pagePrefix}alerts_logs#system_alerts"><u>Alertas de sistema</u></a> siempre están habilitadas.<br>
Las	<a href="${pagePrefix}alerts_logs#member_alerts"><u>Alertas de miembro</u></a> pueden ser configuradas en la 
<a href="${pagePrefix}settings#alerts"><u>página de configuración de alertas</u></a>.
<hr>

<a NAME="system_alerts"></a>
<h3>Alertas de sistema</h3>
Esta ventana muestra una lista con las alertas existentes en el Sistema. <br>
Son alertas relacionadas con el sistema (no directamente relacionados con cuentas de miembros).<br>
<br>
Las siguientes opciones están disponibles:	
<ul>
	<li> El programa se inició 
	<li> El programa finalizó
	<li> Cuenta iniciada 
	<li> Cuenta cancelada
	<li> Cuenta terminada
	<li> Cuenta con fallas
	<li> Cuenta recuperada
	<li> El acceso del administrador 'admin' al sistema está
	temporalmente bloqueado por exceder el máximo número de 
	intentos. La dirección IP es: [la que corresponda]
	<li> La reconstrucción de los índices de búsqueda para Todos fue iniciada
	<li> La reconstrucción de los índices de búsqueda para Todos finalizó
	<li> La reconstrucción de los índices de búsqueda para Miembros fue iniciada
	<li> La reconstrucción de los índices de búsqueda para Miembros finalizó
	<li> El índice de búsqueda para Administradores fue optimizado
	<li> El índice de búsqueda para Miembros fue optimizado
</ul>

Usted puede eliminar alertas del sistema:
<ul> 
<li>Presionando el ícono de Eliminación 
<img border="0" src="${images}/delete.gif" width="16" height="16"> correspondiente;</li> 
<li>Seleccionando una o más alertas, y presionando el botón "Eliminar elemento seleccionado", 
ubicado en la parte inferior derecha de la ventana.</li>
</ul>
<br>
Sin embargo, existe un <a href="#alerts_history"><u>Historial de Alertas</u></a>, donde usted 
podrá buscar antiguas Alertas eliminadas de la lista. 
De esta forma es posible detectar alertas y patrones repetidos.
<hr class='help'>

<a name="member_alerts"></a>
<h3>Alertas de miembro</h3>
Esta ventana muestra una lista con las alertas relacionadas con el comportamiento de los Miembros en el sistema.<br>
Los parámetros pueden establecerse en el "Menú: Configuración>
<a href="${pagePrefix}settings#alerts"> <u>Configuración de alertas.</u></a>"<br>
<br>
Las siguientes opciones se encuentran disponibles:
<ul>
	<li> Miembros que reciben determinado número de malas <a href="${pagePrefix}references"><u>Referencias</u></a>.
	<li> Miembros que dan determinado número de malas referencias.
	<li> Número de días en que un miembro no ha reaccionado frente una factura (sólo facturas del sistema a un miembro).
	<li> Número de facturas rechazadas.
	<li> Cantidad máxima de errores en el inicio de sesión (login) por Código de usuario incorrecto superada
	(varios intentos de acceso/login con un código de usuario incorrecto).
	<li> Usuarios temporalmente bloqueados por superar el número máximo de intentos permitidos.
	<li> Cantidad máxima de errores en el inicio de sesión (login) por Contraseña de acceso incorrecta superada 
	(varios intentos de acceso/login con un código de usuario correcto, pero una contraseña de acceso incorrecta).
	<li> Nuevos miembros (registro público) en la página de acceso.
	<li> Préstamo vencido. Expiración de préstamos que no han sido pagos.
	<li> El <i>código de seguridad del dispositivo de acceso</i> fue bloqueado por exceder <i>n</i> intentos inválidos, para el <i>canal</i> (que corresponda).
	<li> El acceso al sistema está temporalmente bloqueado por exceder el número máximo de intentos. La IP del miembro es: [la que corresponda].
</ul>
Usted puede utilizar el ícono de eliminación 
<img border="0" src="${images}/delete.gif" width="16" height="16"> <b></b> 
correspondiente, para eliminar una alerta de la lista.<br>
<br>
Sin embargo, existe un <a href="#alerts_history"><u>Historial de Alertas</u></a>, donde usted 
podrá buscar antiguas Alertas eliminadas de la lista. 
De esta forma es posible detectar alertas y patrones repetidos.
<hr class='help'>

<A NAME="alerts_history"></A>
<h3>Historial de Alertas</h3>
Esta ventana le permite buscar antiguas alertas, aunque las mismas hayan sido eliminadas.<br> 
<br>
Si usted no completa los filtros disponibles, obtendrá una visión global 
de todas las alertas o avisos existentes (de Sistema o de Miembro).<br> 
<br>
Cuando se selecciona el tipo de alerta de "Miembro", se podrá filtrar por un miembro en particular y 
en la lista de alertas resultante de la búsqueda, se mostrará la identificación del miembro. <br>
También es posible buscar alertas por un rango de fechas, utilizando la selección de <i>Fecha desde</i> y <i>Fecha hasta</i>.<br>
<br>
Si desea visualizar las últimas alertas, puede hacerlo a través del 
"Menú: Alertas> Alertas de Sistema" y "Menú: Alertas> Alertas de miembro".<br>
<hr class='help'>

<a name="alerts_history_result"></a>
<h3> Resultado de búsqueda en historial de alertas </h3>
Esta ventana muestra todas las antiguas alertas que cumplan con los criterios de búsqueda
especificados por usted en la ventana superior.<br> 
<br> 
Pueden estar disponibles varias páginas de resultados, 
en la parte inferior derecha de la ventana usted podrá acceder a cada una de ellas. <br>
<br>
Si desea visualizar las últimas alertas, por favor, vaya al 
"Menú: Alertas> Alertas de sistema" y " Menú: Alertas> Alertas de miembro ". <br>
<hr class="help">

<A NAME="error_log"></A>
<h3> Resultado de búsqueda en el log de errores</h3>
Esta página muestra una lista con todos los errores de aplicación producidos en el sistema. <br>
Usted puede: 
<ul>
	<li>Abrir y visualizar los detalles del error, presionando el ícono de Visualización 
	<img border="0" src="${images}/view.gif" width="16" height="16"> correspondiente.</li>
	<li>Eliminar un error de la lista, presionando el ícono de Eliminación 
	<img border="0" src="${images}/delete.gif" width="16" height="16"> correspondiente.</li>
</ul>
Los errores eliminados permanecerán accesibles en el <a href="#error_history"><u>historial de errores</u></a>.
<hr class='help'>

<a name="error_history"></a>
<h3> Búsqueda en el historial de log de errores </h3>
Esta página le permite especificar un rango de fechas, con el fin de limitar los 
<a href="#error_history_result"><u>resultados de la búsqueda</u></a>. 
<hr class="help">

<A NAME="error_history_result"></A>
<h3>Resultado de búsqueda en el historial de log de errores </h3>
Esta página muestra una lista con todos los errores de aplicación registrados en el período de tiempo 
(rango de fechas) especificado en la 
<a href="#error_history"><u>búsqueda en el historial de log de errores</u></a>.<br>
Si no es especificado un rango de fechas, se mostrará la lista completa de los errores de aplicación producidos.<br>
<br>
Usted puede abrir/visualizar directamente un error de la lista, haciendo click en el ícono de Visualización 
<img border="0" src="${images}/view.gif" width="16" height="16"> correspondiente. 
<br>
Pueden estar disponibles varias páginas de resultados, 
en la parte inferior derecha de la ventana usted podrá acceder a cada una de ellas.<br>
<br>
Los errores sólo aparecen en esta ventana si han sido eliminados del
<a href="#error_log"><u>log de errores</u></a> ("Menú: Alertas> Log de errores").
<hr class='help'>

<a name="error_log_details"></a>
<h3> Detalles de error </h3>
Esta página muestra los detalles del error de aplicación. 
Esta información ayudará al administrador de sistemas y desarrolladores de Cyclos para visualizar las causas del error.
<p>
Nota:</b> Un error de aplicación no es necesariamente un error.
Debido a la flexibilidad de la configuración de Cyclos, es posible configurar un
conflicto con la configuración de funciones. 
La mayoría de este tipo de errores son "atrapados" en Cyclos con un mensaje de error específico, 
pero no es posible prever todas las configuraciones posibles de errores.</p>
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