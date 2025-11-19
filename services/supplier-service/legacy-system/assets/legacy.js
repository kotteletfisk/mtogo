function addItem(event) {
  event.preventDefault();

  const id = document.getElementById("item_id").value.trim();
  const price = parseFloat(document.getElementById("item_price").value).toFixed(
    2
  );
  const amount = document.getElementById("item_amount").value;

  if (!id || !price || !amount) {
    return;
  }

  const subtotal = (price * amount).toFixed(2);
  const table = document.getElementById("order_table").querySelector("tbody");
  const row = document.createElement("tr");

  row.innerHTML = `
      <td>${id}</td>
      <td>${amount}</td>
      <td>${price}</td>
      <td>${subtotal}</td>
    `;

  table.appendChild(row);

  // Clear inputs
  document.getElementById("item_id").value = "";
  document.getElementById("item_price").value = "";
  document.getElementById("item_amount").value = "";

  updateTotal();
}

function updateTotal() {
  const rows = document.querySelectorAll("#order_table tbody tr");
  let total = 0;

  rows.forEach((row) => {
    const subtotalCell = row.children[3];
    const value = parseFloat(subtotalCell.textContent);
    total += value;
  });

  document.getElementById("total_price").textContent = total.toFixed(2);
}

function submitOrder(event) {
  document.getElementById("errmsg").textContent = "";
  const phone = document.getElementById("phone");

  if (!phone.value) {
    document.getElementById("errmsg").textContent = "No phone pumber found!";
    return;
  }

  const rows = document.querySelectorAll("#order_table tbody tr");
  if (rows.length < 1) {
    document.getElementById("errmsg").textContent = "No order items found!";
    return;
  }
  const xmlDoc = document.implementation.createDocument(null, "Order", null);
  const root = xmlDoc.documentElement;

  rows.forEach((row) => {
    const cells = row.querySelectorAll("td");

    const line = xmlDoc.createElement("OrderLine");

    const id = xmlDoc.createElement("ItemId");
    const amount = xmlDoc.createElement("Amount");
    const price = xmlDoc.createElement("UnitPrice");
    const subtotal = xmlDoc.createElement("SubTotal");

    id.textContent = cells[0].textContent;
    amount.textContent = cells[1].textContent;
    price.textContent = cells[2].textContent;
    subtotal.textContent = cells[3].textContent;

    line.appendChild(id);
    line.appendChild(amount);
    line.appendChild(price);
    line.appendChild(subtotal);

    root.appendChild(line);
  });
  const total = xmlDoc.createElement("Total");
  const phone_xml = xmlDoc.createElement("Phone");
  total.textContent = document.getElementById("total_price").textContent;
  phone_xml.textContent = phone.value;
  root.appendChild(total);
  root.appendChild(phone_xml);

  submitXML(xmlDoc)
    .then((result) => {
      console.log("Success:", result.status);
      console.log("Response:", result.response);
      clearOrderTable();
    })
    .catch((error) => {
      console.log("Failed:", error.status);
      console.log("Response:", error.response);
      // clearOrderTable();
    });
}

function submitXML(xmlDoc) {
  const xmlString = new XMLSerializer().serializeToString(xmlDoc);
  console.log(xmlString);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/submit", true);
    xhr.setRequestHeader("Content-Type", "text/xml");

    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        const status = xhr.status;
        const response = xhr.responseText;

        if (status === 0 || (status >= 200 && status < 400)) {
          // The request has been completed successfully
          resolve({ status, response });
        } else {
          reject({ status, response });
        }
      }
    };

    xhr.send(xmlString);
  });
}

function clearOrderTable() {
  const tbody = document.querySelector("#order_table tbody");
  tbody.innerHTML = "";
}
