use axum::Router;
use axum::http::StatusCode;
use axum::routing::post;
use quick_xml::{de, se};
use serde::{Deserialize, Serialize};
use std::error::Error;
use std::io::Write;
use std::net::TcpStream;
use tower_http::services::ServeDir;

#[tokio::main]

async fn main() {
    let app = app();
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    println!("server on {}", listener.local_addr().unwrap());
    axum::serve(listener, app).await.unwrap();
}

fn app() -> Router {
    let static_files = ServeDir::new("./assets");

    Router::new()
        .fallback_service(static_files)
        .route("/submit", post(handle_submit))
}

async fn handle_submit(body: String) -> Result<(StatusCode, String), (StatusCode, String)> {
    match de::from_str::<Order>(&body) {
        Ok(order) => {
            println!("Received Order: {:?}", order);

            match write_to_db(order).await {
                Ok(()) => Ok((StatusCode::CREATED, String::from("Order Created"))),
                Err(e) => Err((StatusCode::INTERNAL_SERVER_ERROR, e.to_string())),
            }
        }
        Err(e) => {
            println!("Failed to serialize order: {}", e);
            Err((StatusCode::BAD_REQUEST, e.to_string()))
        }
    }
}

async fn write_to_db(order: Order) -> Result<(), Box<dyn Error>> {
    let xml_payload = se::to_string(&order)?;
    let mut stream = TcpStream::connect("127.0.0.1:1984")?;

    stream.write_all(xml_payload.as_bytes())?;
    stream.shutdown(std::net::Shutdown::Write)?;

    Ok(())
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "PascalCase")]
struct Order {
    order_line: Vec<OrderLine>,
    total: f32,
    phone: String,
    supplier: i32,
}
#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "PascalCase")]
struct OrderLine {
    item_id: String,
    amount: i32,
    unit_price: f32,
    sub_total: f32,
}
