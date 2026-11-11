CREATE TABLE IF NOT EXISTS barberos (
    id_barbero INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento_identidad VARCHAR(30) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(150),
    fecha_contratacion DATE,
    porcentaje_comision DECIMAL(5,2),
    estado VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_barberos PRIMARY KEY (id_barbero),
    CONSTRAINT uk_barberos_documento UNIQUE (documento_identidad),
    CONSTRAINT chk_barberos_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'VACACIONES')),
    CONSTRAINT chk_barberos_comision CHECK (porcentaje_comision IS NULL OR porcentaje_comision BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clientes (
    id_cliente INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(150),
    fecha_nacimiento DATE,
    notas TEXT,
    estado VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT chk_clientes_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS servicios (
    id_servicio INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    duracion_minutos INT NOT NULL,
    precio DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_servicios PRIMARY KEY (id_servicio),
    CONSTRAINT chk_servicios_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT chk_servicios_precio CHECK (precio >= 0),
    CONSTRAINT chk_servicios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS barbero_servicio (
    id_barbero INT UNSIGNED NOT NULL,
    id_servicio INT UNSIGNED NOT NULL,
    CONSTRAINT pk_barbero_servicio PRIMARY KEY (id_barbero, id_servicio),
    CONSTRAINT fk_barbero_servicio_barbero FOREIGN KEY (id_barbero)
        REFERENCES barberos (id_barbero) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_barbero_servicio_servicio FOREIGN KEY (id_servicio)
        REFERENCES servicios (id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_barbero_servicio_servicio (id_servicio)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS horarios_barbero (
    id_horario INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_barbero INT UNSIGNED NOT NULL,
    dia_semana INT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT pk_horarios_barbero PRIMARY KEY (id_horario),
    CONSTRAINT fk_horarios_barbero_barbero FOREIGN KEY (id_barbero)
        REFERENCES barberos (id_barbero) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_horarios_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horarios_rango CHECK (hora_inicio < hora_fin),
    INDEX idx_horarios_barbero_dia (id_barbero, dia_semana)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS citas (
    id_cita INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_cliente INT UNSIGNED NOT NULL,
    id_barbero INT UNSIGNED NOT NULL,
    fecha_hora_inicio DATETIME NOT NULL,
    fecha_hora_fin DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    observaciones TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_citas PRIMARY KEY (id_cita),
    CONSTRAINT fk_citas_cliente FOREIGN KEY (id_cliente)
        REFERENCES clientes (id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_citas_barbero FOREIGN KEY (id_barbero)
        REFERENCES barberos (id_barbero) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_citas_estado CHECK (estado IN ('PROGRAMADA', 'CONFIRMADA', 'EN_PROCESO', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO')),
    CONSTRAINT chk_citas_rango CHECK (fecha_hora_inicio < fecha_hora_fin),
    INDEX idx_citas_inicio (fecha_hora_inicio),
    INDEX idx_citas_barbero_inicio (id_barbero, fecha_hora_inicio)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cita_servicio (
    id_cita INT UNSIGNED NOT NULL,
    id_servicio INT UNSIGNED NOT NULL,
    cantidad INT NOT NULL,
    precio_aplicado DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_cita_servicio PRIMARY KEY (id_cita, id_servicio),
    CONSTRAINT fk_cita_servicio_cita FOREIGN KEY (id_cita)
        REFERENCES citas (id_cita) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cita_servicio_servicio FOREIGN KEY (id_servicio)
        REFERENCES servicios (id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_cita_servicio_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_cita_servicio_precio CHECK (precio_aplicado >= 0),
    INDEX idx_cita_servicio_servicio (id_servicio)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pagos (
    id_pago INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_cita INT UNSIGNED NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    propina DECIMAL(12,2) NOT NULL DEFAULT 0,
    metodo_pago VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    referencia VARCHAR(255),
    fecha_pago DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pagos PRIMARY KEY (id_pago),
    CONSTRAINT fk_pagos_cita FOREIGN KEY (id_cita)
        REFERENCES citas (id_cita) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_pagos_monto CHECK (monto >= 0),
    CONSTRAINT chk_pagos_propina CHECK (propina >= 0),
    CONSTRAINT chk_pagos_metodo CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'TRANSFERENCIA', 'BILLETERA_DIGITAL')),
    CONSTRAINT chk_pagos_estado CHECK (estado IN ('PENDIENTE', 'PAGADO', 'REEMBOLSADO', 'ANULADO)),
    INDEX idx_pagos_cita (id_cita)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS productos (
    id_producto INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    precio_compra DECIMAL(12,2) NOT NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    cantidad_disponible INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_productos PRIMARY KEY (id_producto),
    CONSTRAINT chk_productos_precio_compra CHECK (precio_compra >= 0),
    CONSTRAINT chk_productos_precio_venta CHECK (precio_venta >= 0),
    CONSTRAINT chk_productos_cantidad CHECK (cantidad_disponible >= 0),
    CONSTRAINT chk_productos_stock_minimo CHECK (stock_minimo >= 0),
    INDEX idx_productos_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS proveedores (
    id_proveedor INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    correo VARCHAR(150),
    direccion VARCHAR(255),
    CONSTRAINT pk_proveedores PRIMARY KEY (id_proveedor),
    INDEX idx_proveedores_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS productos_proveedores (
    id_producto INT UNSIGNED NOT NULL,
    id_proveedor INT UNSIGNED NOT NULL,
    CONSTRAINT pk_productos_proveedores PRIMARY KEY (id_producto, id_proveedor),
    CONSTRAINT fk_productos_proveedores_producto FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_productos_proveedores_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedores (id_proveedor) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_productos_proveedores_proveedor (id_proveedor)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id_movimiento INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_producto INT UNSIGNED NOT NULL,
    cantidad INT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo ENUM('ENTRADA', 'SALIDA') NOT NULL,
    CONSTRAINT pk_movimientos_inventario PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_movimientos_inventario_producto FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_movimientos_cantidad CHECK (cantidad > 0),
    INDEX idx_movimientos_producto_fecha (id_producto, fecha)
) ENGINE=InnoDB;
