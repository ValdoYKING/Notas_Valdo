# Notas Inteligentes Valdo

Aplicación de notas inteligentes desarrollada en Android con Kotlin, Jetpack Compose y Room.

## Características principales
- Crear, editar y eliminar notas con soporte para Markdown.
- Marcar notas como favoritas.
- Organización por categorías y búsqueda de notas.
- Notificaciones programables para recordatorios.
- Interfaz moderna y responsiva con Jetpack Compose.
- Persistencia local usando Room (SQLite).

## Estructura del proyecto

- **MainActivity.kt**: Punto de entrada de la app, inicializa el ViewModel y la navegación.
- **navigation/Navigation.kt**: Define las rutas y transiciones entre pantallas.
- **viewmodel/NoteViewModel.kt**: Lógica de negocio, manejo de estados y operaciones CRUD.
- **data/NoteDatabase.kt, NoteDao.kt, DataBuilder.kt**: Configuración y acceso a la base de datos Room.
- **models/Note.kt**: Modelo de datos principal para las notas.
- **components/NoteCard.kt**: Componente visual para mostrar una nota en la lista.
- **screens/**: Pantallas principales de la app (NotesScreen, NoteDetailScreen, NoteFormScreen).
- **ui/theme/**: Definición de colores y estilos personalizados.

## Instalación y ejecución

1. Clona el repositorio y ábrelo en Android Studio.
2. Sincroniza el proyecto para descargar las dependencias.
3. Ejecuta la app en un emulador o dispositivo físico con Android 9 (API 28) o superior.

## Dependencias clave
- Jetpack Compose
- Room (con KSP)
- Navigation Compose
- Compose Markdown
- Kotlin Coroutines

## Notas técnicas
- El proyecto usa KSP para la generación de código de Room.
- El esquema de la base de datos se exporta en `/app/schemas` para facilitar migraciones.
- El código está documentado y comentado para facilitar su comprensión y mantenimiento.

## Licencia
MIT

