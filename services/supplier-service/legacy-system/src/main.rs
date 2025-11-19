use axum::{  Router, routing::{get, post}};
use tower_http::services::ServeDir;

#[tokio::main]

async fn main() {
    let app = app();
    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    println!("server on {}", listener.local_addr().unwrap());
    axum::serve(listener,app).await.unwrap();
}

fn app () -> Router {
    let static_files = ServeDir::new("./assets");
    
    Router::new()
        .fallback_service(static_files)
        .route("/submit", post(|| async { "Hello, World!" }))
}