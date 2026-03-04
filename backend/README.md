# run the below commands in your terminal inside the project directory you have chosen,
# make sure to have installed uv

uv init backend

cd backend

uv venv

.\.venv\Scripts\Activate

uv add fastapi "uvicorn[standard]"

## here go and copy the main from this repo ( the file will change later bas this works for now )

uv run uvicorn main:app --reload
