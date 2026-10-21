-- KASIR IDRIS: VALIDASI DATA PRODUK DAN ITEM TRANSAKSI
-- Jalankan sekali di Supabase SQL Editor.
-- Aman untuk data lama: NOT VALID tidak memeriksa ulang semua baris lama,
-- tetapi aturan tetap berlaku untuk INSERT dan UPDATE baru.
-- Tidak menghapus/mengubah data yang sudah ada.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.products'::regclass
      AND conname = 'products_price_positive_chk'
  ) THEN
    ALTER TABLE public.products
      ADD CONSTRAINT products_price_positive_chk
      CHECK (price > 0) NOT VALID;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.products'::regclass
      AND conname = 'products_cost_nonnegative_chk'
  ) THEN
    ALTER TABLE public.products
      ADD CONSTRAINT products_cost_nonnegative_chk
      CHECK (cost >= 0) NOT VALID;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.sale_items'::regclass
      AND conname = 'sale_items_price_positive_chk'
  ) THEN
    ALTER TABLE public.sale_items
      ADD CONSTRAINT sale_items_price_positive_chk
      CHECK (price > 0) NOT VALID;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.sale_items'::regclass
      AND conname = 'sale_items_cost_nonnegative_chk'
  ) THEN
    ALTER TABLE public.sale_items
      ADD CONSTRAINT sale_items_cost_nonnegative_chk
      CHECK (cost >= 0) NOT VALID;
  END IF;
END $$;

-- PENTING:
-- 1. Modal 0 tetap diizinkan untuk produk gratis (contoh: Lee mineral).
-- 2. Harga jual harus lebih dari 0.
-- 3. Modal tidak boleh negatif.
-- 4. Modal > harga jual TIDAK ditolak oleh database karena bisa saja disengaja;
--    aplikasi memberi peringatan dan meminta konfirmasi sebelum menyimpan.
-- 5. Constraint ini tidak memperbaiki laba lama. Periksa laporan secara terpisah.
