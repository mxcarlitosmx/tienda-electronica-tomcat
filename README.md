# 🛒 Prototipo de Sistema de Comercio Electrónico (Arquitectura 3 Capas)

Este proyecto es una implementación completa de un sistema de comercio electrónico, desarrollado utilizando una arquitectura de tres capas (Cliente, Servidor de Aplicaciones y Base de Datos). El objetivo principal del desarrollo fue construir un sistema robusto gestionando peticiones RESTful, control de concurrencia y transacciones de base de datos de manera nativa sin el uso de frameworks pesados (como Spring Boot), demostrando un dominio profundo del lenguaje y la infraestructura.

El sistema se encuentra desplegado en un entorno de producción sobre una Máquina Virtual en **Microsoft Azure**.

---

## ✨ Funcionalidades Principales

### 📦 Gestión de Inventario (Back-Office)
* **Alta de Artículos:** Permite registrar nuevos productos en el catálogo especificando nombre, descripción, precio, stock inicial y fotografía.
* **Control de Stock Estricto:** La lógica de backend asegura que los inventarios se actualicen dinámicamente mediante transacciones SQL (`FOR UPDATE`) para evitar condiciones de carrera o ventas sobre-vendidas.

### 🛍️ Tienda Virtual (Front-End)
* **Motor de Búsqueda:** Búsqueda dinámica de artículos por palabra clave (nombre o descripción).
* **Carrito de Compras:** Gestión completa de un carrito por usuario. Permite agregar artículos (validando stock disponible), sumar cantidades, eliminar partidas individuales o vaciar el carrito completo.
* **Autenticación y Seguridad:** Registro de usuarios e inicio de sesión con encriptación de contraseñas mediante el algoritmo criptográfico **SHA-256**.

---

## 🛠️ Tecnologías y Herramientas

**Front-End (Capa de Presentación):**
* HTML5, CSS3 y JavaScript puro (Vanilla JS).
* **Bootstrap 5:** Diseño responsivo y adaptable (Mobile-First) optimizado para dispositivos móviles y tabletas.
* Consumo de API REST nativo mediante `XMLHttpRequest` / `fetch`.

**Back-End (Capa de Negocio):**
* **Java (JDK 8+):** Lógica de negocio e implementación del servicio web.
* **JAX-RS / Jersey:** Exposición de endpoints RESTful con respuestas estrictas en formato JSON.
* **Apache Tomcat:** Servidor web y contenedor de servlets.

**Base de Datos (Capa de Datos):**
* **MySQL:** Diseño relacional con tablas para `usuarios`, `stock`, `fotos_articulos` y `carrito_compra`.
* Integridad de datos controlada mediante llaves foráneas, índices únicos y manejo manual de transacciones (`commit` / `rollback`).

**Infraestructura y Despliegue (Cloud):**
* **Microsoft Azure:** Despliegue en Máquina Virtual (Linux).
* Configuración de puertos, reglas de firewall y red virtual.

---

## ⚙️ Arquitectura de la Base de Datos y Transacciones

El diseño de la base de datos se estructuró para mantener la integridad referencial y soportar operaciones críticas. 

Una de las características técnicas más destacadas del proyecto es el manejo de **Transacciones ACID**. Operaciones complejas, como la compra de un artículo (que implica restar stock de inventario y sumar la entrada al carrito del usuario), se ejecutan desactivando el auto-commit de JDBC. Esto garantiza que, si ocurre un error durante el proceso, se realice un `rollback` automático, protegiendo la consistencia de los datos en producción.
