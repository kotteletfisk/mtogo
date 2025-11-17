BEGIN;

CREATE TYPE orderstatus AS ENUM ('created', 'rejected', 'accepted', 'waiting', 'delivering','delivered');

CREATE TABLE IF NOT EXISTS public.customer
(
    customer_id serial NOT NULL,
    customer_name character varying(50) NOT NULL,
    customer_zip character varying(10) NOT NULL,
    customer_phone character varying(15) NOT NULL,
    customer_creds character varying(50),
    PRIMARY KEY (customer_id)
);

CREATE TABLE IF NOT EXISTS public."order"
(
    order_id serial NOT NULL,
    customer_id serial NOT NULL,
    order_created timestamp without time zone NOT NULL,
    order_updated timestamp without time zone NOT NULL,
    order_status orderstatus NOT NULL,
    PRIMARY KEY (order_id)
);

CREATE TABLE IF NOT EXISTS public.supplier
(
    supplier_id serial NOT NULL,
    supplier_name character varying(50) NOT NULL,
    supplier_zip character varying(10) NOT NULL,
    supplier_creds character varying(50),
    PRIMARY KEY (supplier_id)
);

CREATE TABLE IF NOT EXISTS public.menu_item
(
    item_id serial NOT NULL,
    item_name character varying(50) NOT NULL,
    item_price real NOT NULL,
    supplier_id serial NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (item_id)
);

CREATE TABLE IF NOT EXISTS public.order_line
(
    order_line_id serial NOT NULL,
    order_id serial NOT NULL,
    item_id serial NOT NULL,
    price_snapshot real NOT NULL,
    amount integer NOT NULL DEFAULT 1,
    PRIMARY KEY (order_line_id)
);

CREATE TABLE IF NOT EXISTS public.courier
(
    courier_id serial NOT NULL,
    courier_name character varying(50) NOT NULL,
    courier_creds character varying(50),
    PRIMARY KEY (courier_id)
);

CREATE TABLE IF NOT EXISTS public.courier_order
(
    courier_order_id serial NOT NULL,
    courier_id serial NOT NULL,
    order_id serial NOT NULL,
    PRIMARY KEY (courier_order_id)
);

ALTER TABLE IF EXISTS public."order"
    ADD CONSTRAINT fk_customer_id FOREIGN KEY (customer_id)
    REFERENCES public.customer (customer_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public.menu_item
    ADD CONSTRAINT fk_supplier_id FOREIGN KEY (supplier_id)
    REFERENCES public.supplier (supplier_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public.order_line
    ADD CONSTRAINT fk_order_id FOREIGN KEY (order_id)
    REFERENCES public."order" (order_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public.order_line
    ADD CONSTRAINT fk_item_id FOREIGN KEY (item_id)
    REFERENCES public.menu_item (item_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public.courier_order
    ADD CONSTRAINT fk_courier_id FOREIGN KEY (courier_id)
    REFERENCES public.courier (courier_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public.courier_order
    ADD CONSTRAINT fk_order_id FOREIGN KEY (order_id)
    REFERENCES public."order" (order_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;

END;