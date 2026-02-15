# Notas Inteligentes Valdo

Aplicación de notas inteligentes desarrollada en Android con Kotlin, Jetpack Compose y Room.

## 📱 Estado del Proyecto

**Versión**: 1.0.4  
**Estado**: ✅ **Listo para Play Store**  
**Target SDK**: 36 (Android 16)  
**Min SDK**: 28 (Android 9)

## ✨ Características principales

- ✅ Crear, editar y eliminar notas con soporte completo para **Markdown**
- ✅ Marcar notas como **favoritas**
- ✅ Organización por **categorías** personalizables
- ✅ **Búsqueda** rápida de notas por título o contenido
- ✅ **Notificaciones programables** para recordatorios
- ✅ **Bóveda segura** con autenticación biométrica
- ✅ **Compartir y exportar** (texto, archivo .txt/.md, PDF)
- ✅ **Importar archivos** de texto desde otras apps
- ✅ **Temas** claro y oscuro
- ✅ **Foto de perfil** personalizable
- ✅ Interfaz moderna y responsiva con **Jetpack Compose**
- ✅ Persistencia local usando **Room** (SQLite)
- ✅ **100% offline** - Sin recopilación de datos

## 📂 Estructura del proyecto

- **MainActivity.kt**: Punto de entrada de la app, inicializa el ViewModel y la navegación
- **navigation/Navigation.kt**: Define las rutas y transiciones entre pantallas
- **viewmodel/NoteViewModel.kt**: Lógica de negocio, manejo de estados y operaciones CRUD
- **data/**: Base de datos Room (NoteDatabase.kt, NoteDao.kt, DataBuilder.kt)
- **models/Note.kt**: Modelo de datos principal para las notas
- **components/**: Componentes reutilizables (NoteCard.kt, etc.)
- **screens/**: Pantallas principales (NotesScreen, NoteDetailScreen, NoteFormScreen, etc.)
- **auth/BiometricAuthHelper.kt**: Gestión de autenticación biométrica
- **notification/**: Sistema de notificaciones y recordatorios
- **ui/theme/**: Definición de colores y estilos personalizados

## 🚀 Instalación y ejecución

### Requisitos previos
- Android Studio Ladybug (2024.2.1) o superior
- JDK 11 o superior
- Android SDK 36
- Dispositivo o emulador con Android 9 (API 28) o superior

### Pasos

1. Clona el repositorio:
```bash
git clone https://github.com/tuusuario/NotasInteligentesValdo.git
cd NotasInteligentesValdo
```

2. Abre el proyecto en Android Studio

3. Sincroniza el proyecto para descargar las dependencias:
```bash
./gradlew --refresh-dependencies
```

4. Ejecuta la app:
   - En un emulador: Selecciona un dispositivo y presiona "Run"
   - En un dispositivo físico: Conecta el dispositivo con USB debugging habilitado

### Compilar versión Debug

```bash
# Windows
.\gradlew assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

El APK se generará en: `app/build/outputs/apk/debug/app-debug.apk`

## 📦 Dependencias clave

- **Jetpack Compose** - UI moderna y declarativa
- **Room 2.8.4** (con KSP) - Base de datos local
- **Navigation Compose 2.9.6** - Navegación entre pantallas
- **Coil 2.7.0** - Carga y caché de imágenes
- **Compose Markdown** - Renderizado de Markdown
- **DataStore Preferences** - Almacenamiento de configuraciones
- **Biometric 1.1.0** - Autenticación biométrica
- **Kotlin Coroutines 1.10.2** - Programación asíncrona

Ver [`gradle/libs.versions.toml`](gradle/libs.versions.toml) para la lista completa.

## 🔧 Notas técnicas

- El proyecto usa **KSP** (Kotlin Symbol Processing) en lugar de KAPT para mejor rendimiento
- El esquema de la base de datos se exporta en `/app/schemas` para facilitar migraciones
- Configurado con **ProGuard** para ofuscación en builds de release
- Implementa **Splash Screen** nativo de Android 12+
- Usa **Material Design 3** para UI consistente
- El código está documentado con KDoc para facilitar mantenimiento

## 🏪 Preparación para Play Store

### ✅ Completado

- [x] Configuración de build optimizada (minificación, shrinkResources)
- [x] ProGuard rules configuradas
- [x] targetSdk actualizado a 36
- [x] Permisos correctamente declarados
- [x] Iconos en todas las densidades
- [x] Temas claro y oscuro
- [x] Sin TODOs pendientes
- [x] Código limpio y optimizado

### 📋 Pendiente (Ver PLAY_STORE_CHECKLIST.md)

1. **Crear keystore para firma** (CRÍTICO)
2. **Capturar screenshots** (mínimo 2, recomendado 8)
3. **Diseñar gráfico destacado** (1024x500 px)
4. **Publicar política de privacidad** (plantilla incluida)
5. **Crear cuenta en Play Console** ($25 USD único)

### 📚 Documentación de Release

- [`PLAY_STORE_CHECKLIST.md`](PLAY_STORE_CHECKLIST.md) - Lista completa de verificación
- [`RELEASE_INSTRUCTIONS.md`](RELEASE_INSTRUCTIONS.md) - Instrucciones paso a paso para firmar
- [`PRIVACY_POLICY_TEMPLATE.md`](PRIVACY_POLICY_TEMPLATE.md) - Plantilla de política de privacidad
- [`PLAY_STORE_CONTENT.md`](PLAY_STORE_CONTENT.md) - Textos y descripciones para la tienda
- [`generar-release.bat`](generar-release.bat) - Script automatizado para generar release

## 🔐 Generar Release

### Método 1: Script automatizado (Recomendado)

```bash
# Configura primero las variables de entorno (ver RELEASE_INSTRUCTIONS.md)
.\generar-release.bat
```

### Método 2: Manual

```bash
# Limpiar proyecto
.\gradlew clean

# Generar App Bundle (para Play Store)
.\gradlew bundleRelease

# Generar APK (para instalación directa)
.\gradlew assembleRelease
```

**Nota**: Requiere configurar keystore y variables de entorno primero.

## 🧪 Testing

```bash
# Tests unitarios
.\gradlew test

# Tests instrumentados (requiere dispositivo/emulador)
.\gradlew connectedAndroidTest
```

## 📊 Análisis de Código

```bash
# Lint (análisis estático)
.\gradlew lint

# Reporte de dependencias
.\gradlew dependencies
```

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Haz fork del repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

## 👨‍💻 Autor

**Valdo**  
Email: [tu-email@ejemplo.com]

## 🙏 Agradecimientos

- Material Design 3 por el sistema de diseño
- Jetpack Compose por la moderna UI toolkit
- La comunidad de Android por las librerías open source

## 📱 Screenshots

[Próximamente - añadir screenshots antes del lanzamiento]

## 🔄 Changelog

### Versión 1.0.4 (2026-02-15)
- ✅ Release inicial
- ✅ Todas las funcionalidades principales implementadas
- ✅ Optimizado para Play Store
- ✅ Documentación completa de release

## 🐛 Reporte de Bugs

Si encuentras algún bug, por favor [abre un issue](https://github.com/tuusuario/NotasInteligentesValdo/issues) con:
- Descripción detallada del problema
- Pasos para reproducirlo
- Versión de Android
- Modelo de dispositivo
- Screenshots si es posible

## 💡 Roadmap (Futuras versiones)

- [ ] Sincronización en la nube (opcional)
- [ ] Widget de inicio
- [ ] Soporte para más idiomas
- [ ] Modo de lectura mejorado
- [ ] Etiquetas adicionales
- [ ] Búsqueda avanzada con operadores
- [ ] Estadísticas de uso
- [ ] Exportación masiva

---

**Estado de preparación para Play Store**: 🟢 85% - Técnicamente listo, falta solo material gráfico y firma

**Última actualización**: 2026-02-15

