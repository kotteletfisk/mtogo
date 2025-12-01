BEGIN;

-- Optional: wipe existing data for a clean slate
TRUNCATE TABLE
    courier_order,
    order_line,
    "orders",
    menu_item,
    courier,
    supplier,
    customer,
    auth_user_role,
    auth_user
RESTART IDENTITY CASCADE;

-- =====================================================
-- 1. Customers
-- =====================================================

INSERT INTO public.customer (customer_name, customer_zip, customer_phone, customer_creds)
VALUES
    ('Alice Andersen',    '2100', '+45 11111111', 'alice@example.com'),
    ('Bob Bæk',           '2200', '+45 22222222', 'bob@example.com'),
    ('Charlotte Christensen', '2300', '+45 33333333', 'charlotte@example.com');

-- IDs will be:
-- 1 = Alice
-- 2 = Bob
-- 3 = Charlotte


-- =====================================================
-- 2. Suppliers
-- =====================================================

INSERT INTO public.supplier (supplier_name, supplier_zip, supplier_creds)
VALUES
    ('Pizza Palace',      '2100', 'pizza@example.com'),
    ('Burger Barn',       '2200', 'burger@example.com'),
    ('Sushi Spot',        '2300', 'sushi@example.com');

-- IDs:
-- 1 = Pizza Palace
-- 2 = Burger Barn
-- 3 = Sushi Spot


-- =====================================================
-- 3. Menu items
-- =====================================================

INSERT INTO public.menu_item (item_name, item_price, supplier_id, is_active)
VALUES
    -- Pizza Palace
    ('Margherita Pizza',      75.0, 1, true),
    ('Pepperoni Pizza',       85.0, 1, true),

    -- Burger Barn
    ('Classic Burger',        90.0, 2, true),
    ('Cheeseburger',          95.0, 2, true),

    -- Sushi Spot
    ('Salmon Nigiri (2 pcs)', 40.0, 3, true),
    ('California Roll (8 pcs)', 80.0, 3, true);

-- IDs:
-- 1 = Margherita
-- 2 = Pepperoni
-- 3 = Classic Burger
-- 4 = Cheeseburger
-- 5 = Salmon Nigiri
-- 6 = California Roll


-- =====================================================
-- 4. Couriers
-- =====================================================

INSERT INTO public.courier (courier_name, courier_creds)
VALUES
    ('Carl Courier', 'carl@example.com'),
    ('Dorthe Delivery', 'dorthe@example.com');

-- IDs:
-- 1 = Carl
-- 2 = Dorthe


-- =====================================================
-- 5. Orders
-- =====================================================
-- Use fixed UUIDs so you can line them up with logs if you want

INSERT INTO public."orders" (order_id, customer_id, order_created, order_updated, order_status)
VALUES
    -- Order 1: Alice, just created
    ('f26ca635-d679-41e5-87b9-f0c10798b2d6', 1,
     NOW() - INTERVAL '30 minutes',
     NOW() - INTERVAL '25 minutes',
     'created'),

    -- Order 2: Bob, delivering
    ('09fe8541-6184-4e2e-b425-785deb51a258', 2,
     NOW() - INTERVAL '1 hour',
     NOW() - INTERVAL '10 minutes',
     'delivering'),

    -- Order 3: Charlotte, delivered
    ('11111111-2222-3333-4444-555555555555', 3,
     NOW() - INTERVAL '2 hours',
     NOW() - INTERVAL '30 minutes',
     'delivered');


-- =====================================================
-- 6. Order lines
-- =====================================================

INSERT INTO public.order_line (order_id, item_id, price_snapshot, amount)
VALUES
    -- Order 1 (Alice) - 2x Margherita, 1x Pepperoni
    ('f26ca635-d679-41e5-87b9-f0c10798b2d6', 1, 75.0, 2),
    ('f26ca635-d679-41e5-87b9-f0c10798b2d6', 2, 85.0, 1),

    -- Order 2 (Bob) - 1x Classic Burger, 1x Cheeseburger
    ('09fe8541-6184-4e2e-b425-785deb51a258', 3, 90.0, 1),
    ('09fe8541-6184-4e2e-b425-785deb51a258', 4, 95.0, 1),

    -- Order 3 (Charlotte) - 2x Nigiri, 1x California Roll
    ('11111111-2222-3333-4444-555555555555', 5, 40.0, 2),
    ('11111111-2222-3333-4444-555555555555', 6, 80.0, 1);


-- =====================================================
-- 7. Courier ↔ order mapping
-- =====================================================

INSERT INTO public.courier_order (courier_id, order_id)
VALUES
    -- Carl handles Order 1 & 2
    (1, 'f26ca635-d679-41e5-87b9-f0c10798b2d6'),
    (1, '09fe8541-6184-4e2e-b425-785deb51a258'),

    -- Dorthe handles Order 3
    (2, '11111111-2222-3333-4444-555555555555');


COMMIT;
