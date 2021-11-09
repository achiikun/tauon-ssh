package muon.app.common.i18n;

import java.util.ListResourceBundle;

public class Messages_es extends ListResourceBundle {

    @Override
    protected Object[][] getContents() {

        return resources;
    }

    private final Object[][] resources = {
        
        {"general", "General"},
        {"terminal", "Terminal"},
        {"editor", "Editor"},
        {"display", "Pantalla"},
        {"security", "Seguridad"},
            {"sessions", "Sesiones"},
            {"settings", "Configuraciones"},
            {"add", "Agregar"},
            {"chk_update", "Revisar actualizaciones"},
            {"new_site", "Nuevo sitio"},
            {"new_folder", "Nueva carpeta"},
            {"duplicate", "Duplicar"},
            {"connect", "Conectar"},
            {"cancel", "Cancelar"},
            {"export", "Exportar"},
            {"import", "Importar"},
            {"name", "Nombre"},
            {"connecting", "Conectando..."},
            {"import_from", "Importar desde"},
            {"import_sessions", "Importar sesiones"},
            {"save", "Guardar"},
            {"reset", "Restaurar"},
            {"remove", "Eliminar"},
            {"edit", "Editar"},
            {"generate", "Generar"},
            {"new_entry", "Nueva entrada"},
            {"connection", "Conexion"},
            {"directories", "Directorios"},
            {"proxy", "Proxy"},
            {"jump_hosts", "Jump Hosts"},
            {"port_forwarding", "Port Forwarding"},
            {"user", "Usuario"},
            {"host", "Servidor"},
            {"port", "Puerto"},
            {"password", "Clave"},
            {"local_folder", "Carpeta local"},
            {"remote_folder", "Carpeta remota"},
            {"browse", "Buscar"},
            {"session_manager", "Administrador de sesiones"},
            {"private_key_file", "Archivo de llave privada"},
            {"proxy_type", "Tipo de proxy"},
            {"proxy_host", "Servidor proxy"},
            {"proxy_port", "Puerto proxy"},
            {"proxy_user", "Usuario proxy"},
            {"proxy_password", "Clave proxy"},
            {"warning_plain_text", " ( Advertencia: sera guardado en texto plano! )"},
            {"overwrite", "Sobreescribir"},
            {"auto_rename", "Auto-renombrar"},
            {"skip", "Ignorar"},
            {"prompt", "Preguntar"},
            {"settings_saved", "Se restablecieron los ajustes, \npor favor guarde y reinicie la aplicación"},
            {"columns", "Columnas"},
            {"rows", "Filas"},
            {"font_name", "Nombre fuente"},
            {"font_size", "Tamaño fuente"},
            {"copy_like_putty", "Copiar y pegar como PuTTY (copiar al seleccionar y pegar al hacer clic derecho)"},
            {"terminal_type", "Tipo de terminal"},
            {"initial_terminal_type", "Tamaño inicial de la terminal"},
            {"sound", "Sonido"},
            {"terminal_font", "Fuente Terminal"},
            {"misc", "Misc"},
            {"terminal_colors", "Colores y temas de la terminal"},
            {"terminal_theme", "Tema de la terminal"},
            {"default_color", "Color por defecto"},
            {"text", "Texto"},
            {"background", "Background"},
            {"selection_color", "Seleccion color"},
            {"search_pattern", "Patron de busqueda"},
            {"color_palette", "Paleta de Color"},
            {"terminal_shortcuts", "Atajos de terminal"},
            {"confirm_delete_files", "Confirmar antes de eliminar archivos"},
            {"confirm_move_files", "Confirmar antes de mover o copiar archivos"},
            {"show_hidden_files", "Mostrar archivos ocultos por defecto"},
            {"prompt_for_sudo", "Preguntar por sudo si la operacion falla debido a permisos (cuidado!)"},
            {"directory_caching", "Usar caching de directorios"},
            {"current_folder", "Mostrar carpeta actual en la ruta de la barra"},
            {"show_banner", "Mostrar banner"},
            {"word_wrap", "Word wrap on log viewer"},
            {"transfer_normally", "Transferencia normal"},
            {"transfer_background", "Transferencia en background"},
            {"transfer_mode", "Modo de transferencia"},
            {"conflict_action", "Accion en conflicto"},
            {"add_editor", "+ Agregar editor"},
            {"remove_editor", "- Eliminar editor"},
            {"editor_name", "Nombre editor"},
            {"add_editor2", "Add editor?"},
            {"zoom_text", "Zoom (Hacer que la aplicacion parezca grande o pequeña en pantalla"},
            {"global_dark_theme", "Usar tema oscuro global (necesita reiniciar)"},
            {"zoom_percentage", "Porcentaje de Zoom"},
            {"new_master_password", "Nueva clave maestra"},
            {"reenter_master_password", "Reingrese la clave maestra"},
            {"master_password", "Clave maestra"},
            {"use_master_password", "Usar clave maestra"},
            {"change_master_password", "Cambiar clave maestra"},
            {"change_password_failed", "Cambio de clave fallo!"},
            {"error_operation", "Error durante la operacion"},
            {"password_aes", "Tus claves guardadas estan protegidas por el cifrado AES"},
            {"password_unprotected", "Tus claves ya no estan protegidas"},
            {"password_no_match", "Las claves no coinciden"},
            {"unsupported_key", "Este formato de clave no es compatible, conviértalo al formato OpenSSH"},
            {"copy", "Copiar"},
            {"paste", "Pegar"},
            {"select_all", "Seleccionar todo"},
            {"clear_buffer", "Limpiar buffer"},
            {"find", "Encontrar"},
            {"cut", "Cortar"},
            {"path_executable", "Ruta del ejecutable"},
            {"file_browser", "Buscador de archivo"},
            {"server_logs", "Logs servidor"},
            {"file_search", "Buscar archivo"},
            {"diskspace", "Espacio disco"},
            {"toolbox", "Herramientas"},
            {"processes", "Procesos"},
            {"pid", "PID"},
            {"total_processes", "Total procesos:"},
            {"last_updated", "Ultima actualizacion:"},
            {"analize_folder", "Analizar carpeta"},
            {"analize_volume", "Analizar volumen"},
            {"next", "Siguiente"},
            {"back", "Regresar"},
            {"reload", "Recargar"},
            {"modified", "Modificado"},
            {"size", "Tamaño"},
            {"type", "Tipo"},
            {"permission", "Permisos"},
            {"owner", "Dueño"},
            {"show_hidden_files2", "Mostrar archivos ocultos"},
            {"bookmarks", "Marcadores"},
            {"select_partition", "Por favor seleccione una particion"},
            {"select_volume", "Por favor seleccione un volumen"},
            {"filesystem", "Sistema archivos"},
            {"total_size", "Tamaño total"},
            {"used", "Uso"},
            {"available", "Disponible"},
            {"percentage_use", "% usado"},
            {"mount_point", "Punto montaje"},
            {"directory_usage", "Espacio utilizado por el directorio"},
            {"start_another_analysis", "Iniciar otro analisis"},
            {"delete", "Eliminar"},
            {"send_files", "Enviar archivos"},
            {"add_from_manager", "Agregar desde el administrador de sesiones"},
            {"add_log", "Agregar log"},
            {"insert", "Insertar"},
            {"open", "Abrir"},
            {"rename", "Renombrar"},
            {"new_file", "Nuevo archivo"},
            {"bookmark", "Marcador"},
            {"open_new_tab", "Abrir en una pestaña nueva"},
            {"enter_new_name", "Por favor ingrese el nuevo nombre"},
            {"open_in_terminal", "Abrir en terminal"},
            {"copy_path", "Copiar ruta"},
            {"searching", "Buscando"},
            {"idle", "Inactivo"},
            {"in_filename", "En nombre archivo (como *.zip o R*ME.txt)"},
            {"in_filecontent", "En el contenido del archivo"},
            {"in_compressed_files", "Buscar en archivos comprimidos"},
            {"search_for", "Buscar por"},
            {"search_in", "Buscar en"},
            {"any_time", "Cualquier momento"},
            {"this_week", "Esta semana"},
            {"between", "Entre"},
            {"from", "Desde"},
            {"to", "Hasta"},
            {"ready", "Listo"},
            {"look_for", "Buscar por"},
            {"both_file_folder", "Por archivo y carpeta"},
            {"file_only", "Solo archivo"},
            {"folder_only", "Solo carpeta"},
            {"show_location", "Mostrar ubicacion"},
            {"filename", "Nombre archivo"},
            {"path", "Ruta"},
            {"filter", "Filtro"},
            {"clear", "Limpiar"},
            {"refresh", "Actualizar"},
            {"kill", "Matar"},
            {"kill_sudo", "Matar usando sudo"},
            {"change_priority", "Cambiar prioridad"},
            {"change_priority_sudo", "Cambiar prioridad con sudo"},
            {"copy_command", "Comando copiar"},
            {"kill_process", "Matar proceso"},
            {"system_info", "Info. Sistema"},
            {"system_load", "Carga sistema"},
            {"services_systemd", "Servicios - systemd"},
            {"process_ports", "Procesos y puertos"},
            {"ssh_keys", "Llaves ssh"},
            {"network_tools", "Herramientas de red"},
            {"cpu_usage", "Uso cpu"},
            {"memory_usage", "Uso memoria"},
            {"swap_usage", "Uso swap"},
            {"used2", "Usado"},
            {"generate_new_key", "Generar nueva llave"},
            {"public_key_file", "Archivo de llave publica:"},
            {"refresh_interval", "Intervalo de actualizacion"},
            {"start", "Iniciar"},
            {"stop", "Detener"},
            {"restart", "Reiniciar"},
            {"reload", "Recargar"},
            {"enable", "Habilitar"},
            {"disable", "Deshabilitar"},
            {"actions_sudo", "Realizar acciones con super usuario (sudo)"},
            {"operation_failed", "Operacion fallida"},
            {"status", "Status"},
            {"state", "Estado"},
            {"description", "Descripcion"},
            {"source_port", "Puerto inicio"},
            {"target_port", "Puerto destino"},
            {"bind_host", "Servidor a unir"},
            {"local", "Local"},
            {"remote", "Remoto"},
            {"invalid_input", "Ingreso invalid: todos los campos son obligatorios"},
            {"host_ping", "Ping al servidor"},
            {"host_name", "Nombre del servidor"},
            {"port_number", "Numero de puerto"},
            {"tool_use", "Herramienta a usar"},
            {"executed_errors", "Ejecutado con errores"},
            {"configure_editor", "Editor de configuracion"},
            {"open_log_viewer", "Abrir con el visor de logs"},
            {"open_in_tab", "Abrir en nueva pestaña"},
            {"open_with", "Abrir con..."},
            {"send_another_server", "Enviar por otro servidor"},
            {"send_over_ftp", "Enviar por ftp"},
            {"send_this_computer", "Enviar a este computador"},
            {"run_in_terminal", "Ejecutar en terminal"},
            {"open_folder_terminal", "Abrir carpeta en terminal"},
            {"open_terminal_here", "Abri terminal aqui"},
            {"run_file_in_terminal", "Ejecutar archivo en terminal"},
            {"run_file_in_background", "Ejecutar archivo en background"},
            {"edit_with", "Edit con"},
            {"properties", "Propiedades"},
            {"create_link", "Crear vinculo"},
            {"extract_here", "Extraer aqui"},
            {"extract_to", "Extraer a"},
            {"selec_target", "Seleccion una carpeta de destino a extraer"},
            {"create_archive", "Crear archivo"},
            {"download_files", "Descargar archivos seleccionados"},
            {"upload_here", "Subir archivos aqui"},
            {"please_new_name", "Por favor ingrese el nuevo nombre"},
            {"open_as_url", "Abrir como url"},
            {"page_up", "Page up"},
            {"page_down", "Page down"},
            {"line_up", "Line up"},
            {"line_down", "Line down"},
            {"search", "Buscar"},
            {"no_entry_selected", "No se ha seleccionado ningun entrada"},
            {"authorized_keys", "Llaves autorizadas"},
            {"local_computer", "Llaves autorizadas"},
            {"server", "Servidor"},
            {"language", "Idioma"},
            {"log_viewer_lines", "Lineas por pagina para el visor de logs" },
            {"log_viewer_font_size", "Tamaño de fuente para visor de logs"},
            {"system_refresh_interval", "Intervalo de tiempo de refresco (seg) "},
            {"autorename", "Autorenombrar"},
            {"use_sudo_if_fails", "Usar sudo si la operacion falla por permisos (cuidado!)"},
            {"transfer_temporary_directory", "Transfiera archivos al directorio temporal cuando la operación falla debido a problemas de permisos"},
            {"show_filebrowser_first", "Mostrar primero la pestaña Explorador de archivos"},
            {"connection_timeout", "Tiempo limite de conexion"}

    };
}
