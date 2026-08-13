-- Añadir imagen/foto al insumo (inventario) como URL
ALTER TABLE supplies ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
